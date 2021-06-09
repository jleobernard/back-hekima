package com.leo.hekima.handler;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.model.NoteModel;
import com.leo.hekima.model.NoteTagModel;
import com.leo.hekima.model.SourceModel;
import com.leo.hekima.model.TagModel;
import com.leo.hekima.repository.NoteRepository;
import com.leo.hekima.repository.NoteTagRepository;
import com.leo.hekima.repository.SourceRepository;
import com.leo.hekima.repository.TagRepository;
import com.leo.hekima.to.AckResponse;
import com.leo.hekima.to.HekimaUpsertRequest;
import com.leo.hekima.to.MyPage;
import com.leo.hekima.to.NoteView;
import com.leo.hekima.utils.DataUtils;
import com.leo.hekima.utils.StringUtils;
import com.leo.hekima.utils.WebUtils;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.leo.hekima.handler.UserService.getAuthentication;
import static com.leo.hekima.utils.ReactiveUtils.optionalEmptyDeferred;
import static com.leo.hekima.utils.ReactiveUtils.orEmptyList;
import static com.leo.hekima.utils.WebUtils.getPageAndSort;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class NoteService {
    private static final Logger logger = LoggerFactory.getLogger(NoteService.class);
    private final NoteRepository noteRepository;
    private final NoteTagRepository noteTagRepository;
    private final TagRepository tagRepository;
    private final SourceRepository sourceRepository;
    private final ConnectionFactory connectionFactory;
    private final File dataDir;
    private final String googleCredentialsPath;
    private ImageAnnotatorClient vision;

    public NoteService(NoteRepository noteRepository,
                       NoteTagRepository noteTagRepository, TagRepository tagRepository,
                       SourceRepository sourceRepository,
                       ConnectionFactory connectionFactory,
                       @Value("${google.credentials}") final String googleCredentialsPath,
                       @Value("${data.dir}") final String dataDirPath) {
        this.noteRepository = noteRepository;
        this.noteTagRepository = noteTagRepository;
        this.tagRepository = tagRepository;
        this.sourceRepository = sourceRepository;
        this.connectionFactory = connectionFactory;
        this.dataDir = new File(dataDirPath);
        this.googleCredentialsPath = googleCredentialsPath;
        if(!this.dataDir.exists()) {
            logger.info("Creating data directory {}", dataDirPath);
            if(!this.dataDir.mkdirs()) {
                throw new UnrecoverableServiceException("Cannot create data dir path " + dataDirPath);
            }
            logger.info("Directory {} created successfully", dataDirPath);
        }
        if(!this.dataDir.isDirectory()) {
            throw new UnrecoverableServiceException("Data dir path " + dataDirPath + " is not a directory");
        }
        if(!this.dataDir.canWrite()) {
            throw new UnrecoverableServiceException("Data dir path " + dataDirPath + " is not writable");
        }
        initVision();
    }

    @Transactional(readOnly = true)
    public Mono<ServerResponse> search(ServerRequest request) {
        return Mono.from(connectionFactory.create()).flatMap(connection -> {
            final List<String> conditions = new ArrayList<>();
            var pageAndSort = getPageAndSort(request);
            // TODO Sanitize
            request.queryParam("source")
                .filter(n -> !n.isBlank())
                .map(n -> n.toLowerCase(Locale.ROOT).trim())
                .filter(n -> n.matches("[a-z0-9]+"))
                .ifPresent(s -> conditions.add("note.source_id = (SELECT id FROM source WHERE uri = '" + s + "')"));
            request.queryParam("tags")
                .filter(StringUtils::isNotEmpty)
                .map(tu -> Arrays.stream(tu.split("\\s*,\\s*"))
                        .map(DataUtils::sanitize)
                        .filter(n -> n.matches("[a-z0-9]+"))
                        .collect(Collectors.toList()))
                .filter(l -> !l.isEmpty())
                .ifPresent(s -> conditions.add("note.id IN (SELECT note_id FROM note_tag LEFT JOIN tag on tag.id = note_tag.tag_id WHERE tag.uri in ('" + String.join("','", s) + "'))"));
            final StringBuilder sql = new StringBuilder("SELECT id, uri, valeur, created_at, source_id, mime_type, file_id from note ");
            if(!conditions.isEmpty()) {
                sql.append(" WHERE ");
                sql.append(String.join(" AND ", conditions));
            }
            sql.append(" ORDER BY created_at DESC OFFSET ");
            sql.append(pageAndSort.offset());
            sql.append(" LIMIT ");
            sql.append(pageAndSort.count());
            var views = orEmptyList(
                Flux.from(connection.createStatement(sql.toString()).execute())
                .doFinally((st) -> Mono.from(connection.close()).subscribe())
                .flatMap(result -> result.map((row, meta) -> new NoteModel(
                    row.get("id", Long.class),
                    row.get("uri", String.class),
                    row.get("valeur", String.class),
                    row.get("created_at", Instant.class),
                    row.get("source_id", Long.class),
                    row.get("mime_type", String.class),
                    row.get("file_id", String.class)
                )))
            ).flatMap(notes -> {
                var uris = notes.stream().map(NoteModel::getUri).collect(Collectors.toList());
                var transformers = notes.stream()
                    .map(this::toView)
                    .collect(Collectors.toList());
                return Mono.zip(transformers, aggregat -> Arrays.stream(aggregat).map(a -> (NoteView)a).collect(Collectors.toList()))
                    .map(unorderdNoteViews -> {
                        final List<NoteView> sorted = new ArrayList<>(unorderdNoteViews);
                        sorted.sort(Comparator.comparingInt(h -> uris.indexOf(h.uri())));
                        return sorted;
                    });
            });
            return WebUtils.ok().body(views, NoteView.class);
        });
    }


    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return noteRepository.findByUri(uri)
            .doOnNext(this::deleteFile)
            .flatMap(noteRepository::delete)
            .flatMap(value -> {
                logger.info("Note {} a bien été supprimée", uri);
                return noContent().build();
            })
            .onErrorResume(e -> {
                logger.error("Erreur lors de la suppression", e);
                return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }


    @Transactional
    public Mono<ServerResponse> findByUri(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return noteRepository.findByUri(uri)
            .flatMap(this::toView)
            .flatMap(value -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(value)));
    }

    @Transactional
    public Mono<ServerResponse> uploadFile(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return noteRepository.findByUri(uri).zipWith(serverRequest.multipartData())
            .flatMap(tuple -> {
                final NoteModel hekima = tuple.getT1();
                final MultiValueMap<String, Part> d = tuple.getT2();
                deleteFile(hekima);
                Part file = d.get("file").get(0);
                final String newFileId = StringUtils.md5InHex(uri +  System.currentTimeMillis());
                logger.info("Writing file " + newFileId);
                hekima.setFileId(newFileId);
                hekima.setMimeType(Objects.requireNonNull(file.headers().getContentType()).toString());
                final File newFile = getDataFile(newFileId);
                try {
                    newFile.getParentFile().mkdirs();
                    newFile.createNewFile();
                } catch (IOException e) {
                    logger.error("Could not create file {}", newFile, e);
                    throw new UnrecoverableServiceException("Cannot upload file");
                }
                return file.content().doOnNext(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    try {
                        Files.write(newFile.toPath(), bytes, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new UnrecoverableServiceException("Cannot write new file", e);
                    }
                }).then(noteRepository.save(hekima));
            })
            .flatMap(value -> WebUtils.ok().body(toView(value), NoteView.class));
    }

    @Transactional
    public Mono<ServerResponse> deleteFile(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return noteRepository.findByUri(uri).zipWith(serverRequest.multipartData())
            .flatMap(tuple -> {
                final NoteModel hekima = tuple.getT1();
                final MultiValueMap<String, Part> d = tuple.getT2();
                deleteFile(hekima);
                return noteRepository.save(hekima);
            })
            .flatMap(value -> ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(toView(value))));
    }

    private void deleteFile(NoteModel hekima) {
        if(hasText(hekima.getFileId())) {
            logger.info("Renaming old file {} so it appears deleted", hekima.getFileId());
            final String fileId = hekima.getFileId();
            hekima.setFileId(null);
            hekima.setMimeType(null);
            final File oldFile = getDataFile(fileId);
            if(oldFile.exists()) {
                final boolean renameSuccess = oldFile.renameTo(getDataFile(fileId + "." + System.currentTimeMillis() + ".old"));
                if (!renameSuccess) {
                    throw new UnrecoverableServiceException("Could not rename file " + fileId);
                }
            } else {
                logger.warn("File {} does not exist anymore", fileId);
            }
        } else {
            logger.info("No file to delete for note {}", hekima.getUri());
        }
    }

    @Transactional(readOnly = true)
    public Mono<ServerResponse> getFile(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return noteRepository.findByUri(uri)
            .filter(hekima -> hasText(hekima.getFileId()))
            .map(hekima -> Pair.of(getDataFile(hekima.getFileId()), hekima.getMimeType()))
            .filter(value -> value.getFirst().exists())
            .flatMap(value -> {
                try {
                    return ok()
                        .contentType(MediaType.parseMediaType(value.getSecond()))
                        .body(BodyInserters.fromResource(new InputStreamResource(new FileInputStream(value.getFirst()))));
                } catch (FileNotFoundException e) {
                    logger.error("Cannot read file " + value.getFirst().getAbsolutePath());
                    throw new UnrecoverableServiceException("Cannot read file", e);
                }
            }).switchIfEmpty(notFound().build());
    }

    @Transactional
    public Mono<ServerResponse> upsert(ServerRequest serverRequest) {
        final var now = Instant.now();
        return serverRequest.body(toMono(HekimaUpsertRequest.class))
            .flatMap(request ->  {
                final String uri = StringUtils.isNotEmpty(request.getUri()) ? request.getUri() : WebUtils.getOrCreateUri(request, serverRequest);
                return Mono.zip(
                    Mono.just(request),
                    noteRepository.findByUri(uri).switchIfEmpty(Mono.defer(() -> Mono.just(new NoteModel(uri))))
                );
            })
            .flatMap(uriAndSource -> {
                NoteModel hekima = uriAndSource.getT2();
                final Mono<Tuple2<HekimaUpsertRequest, NoteModel>> deletionTags;
                hekima.setSourceId(null);
                if(hekima.getId() == null) {
                    deletionTags = Mono.just(uriAndSource);
                } else {
                    deletionTags = noteRepository.deleteLinkWithTags(hekima.getId()).then(Mono.just(uriAndSource));
                }
                return deletionTags;
            })
            .flatMap(uriAndSource -> {
                final HekimaUpsertRequest request = uriAndSource.getT1();
                NoteModel note = uriAndSource.getT2();
                note.setValeur(request.getValeur());
                note.setCreatedAt(Instant.now());
                return (request.getSource() == null ?
                        Mono.just(Optional.ofNullable((SourceModel) null)) :
                        optionalEmptyDeferred(sourceRepository.findByUri(request.getSource())))
                .flatMap(maybeSource -> {
                    maybeSource.ifPresent(s -> {
                        note.setSourceId(s.getId());
                        s.setLastUsed(Instant.now());
                        sourceRepository.save(s).subscribe();
                    });
                    return noteRepository.save(note);
                })
                .flatMap(savedNote ->
                    isEmpty(request.getTags()) ? Mono.just(savedNote) :
                        orEmptyList(tagRepository.findByUriIn(request.getTags()))
                        .flatMap(tags -> {
                            final var links = tags.stream().map(tag -> new NoteTagModel(savedNote.getId(), tag.getId())).collect(Collectors.toList());
                            for (TagModel tag : tags) {
                                tag.setLastUsed(now);
                            }
                            tagRepository.saveAll(tags).subscribe();
                            return noteTagRepository.saveAll(links).then(Mono.just(savedNote));
                        })
                )
                .flatMap(savedNote -> WebUtils.ok().body(toView(savedNote), NoteView.class));
            });
    }

    private File getDataFile(String fileId) {
        final String prefix1 = fileId.substring(0,2);
        final String prefix2 = fileId.substring(2,4);
        return new File(dataDir, prefix1 + "/" + prefix2 + "/" + fileId);
    }

    public Mono<NoteView> toView(NoteModel t) {
        return Mono.zip(
            orEmptyList(tagRepository.findByNoteId(t.getId())),
            t.getSourceId() == null ?
                Mono.just(Optional.empty()) :
                sourceRepository.findById(t.getSourceId()).map(Optional::of).switchIfEmpty(Mono.just(Optional.empty())))
        .map(tuple -> {
            final var tags = TagService.toView(tuple.getT1());
            final var source = tuple.getT2().map(s -> SourceService.toView((SourceModel) s)).orElse(null);
            return new NoteView(t.getUri(), t.getValeur(), tags, source, hasText(t.getFileId()));
        });
    }

    private void initVision(final boolean failOnError) {
        try {
            final var myCredentials = ServiceAccountCredentials.fromStream(
                    new FileInputStream(googleCredentialsPath));
            vision = ImageAnnotatorClient.create(ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
                    .build());
        } catch (IOException e) {
            if(failOnError) {
                throw new UnrecoverableServiceException("Cannot initialize vision API", e);
            } else {
                logger.error("Cannot initialize vision API", e);
            }
        }
    }
    private void initVision() {
        initVision(false);
    }

    public Mono<ServerResponse> parseNote(ServerRequest serverRequest) {
        if(vision == null) {
            initVision(true);
        }
        return Mono.zip(
                getAuthentication(),
                serverRequest.multipartData().flatMap(d -> readFileAsByteArray(d.get("file").get(0))))
                .flatMap(tuple -> {
                    final String auth = tuple.getT1().getName();
                    logger.debug("{} wants to analyze a picture", auth);
                    final var data = tuple.getT2();
                    // Builds the image annotation request
                    final List<AnnotateImageRequest> requests = new ArrayList<>();
                    final Image img = Image.newBuilder().setContent(ByteString.copyFrom(data)).build();
                    final Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
                    AnnotateImageRequest request =
                            AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
                    requests.add(request);
                    // Performs label detection on the image file
                    BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
                    List<AnnotateImageResponse> responses = response.getResponsesList();
                    if(responses.isEmpty()) {
                        return ok().bodyValue(AckResponse.OK);
                    } else {
                        return responses.stream()
                        .filter(r -> !r.hasError())
                        .findFirst()
                        .map(res -> {
                            final TextAnnotation fullTextAnnotation = res.getFullTextAnnotation();
                            final MyPage page = toMyObjects(fullTextAnnotation);
                            return ok().bodyValue(page);
                        }).orElseGet(() -> ok().bodyValue(AckResponse.OK));
                    }
                });
    }

    private MyPage toMyObjects(TextAnnotation fullTextAnnotation) {
        return new MyPage(fullTextAnnotation.getPages(0).getBlocksList().stream().flatMap(googleBlock ->
                googleBlock.getParagraphsList().stream().map(googleParagraph ->
                        googleParagraph.getWordsList().stream().map(w -> w.getSymbolsList().stream().map(Symbol::getText).collect(Collectors.joining( "")))
                                .collect(Collectors.joining(" "))
                )
        ).collect(Collectors.toList()));
    }

    private Mono<byte[]> readFileAsByteArray(final Part file) {
        return file.content().collectList()
            .map(dataBuffers -> {
                int totalSize = 0;
                for (DataBuffer dataBuffer : dataBuffers) {
                    totalSize += dataBuffer.readableByteCount();
                }
                byte[] bytes = new byte[totalSize];
                int offset = 0;
                for (DataBuffer dataBuffer : dataBuffers) {
                    final int count = dataBuffer.readableByteCount();
                    dataBuffer.read(bytes, offset, count);
                    offset += count;
                }
                return bytes;
            });
    }
}
