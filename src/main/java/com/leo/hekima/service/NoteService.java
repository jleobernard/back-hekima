package com.leo.hekima.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.model.Word;
import com.leo.hekima.model.*;
import com.leo.hekima.repository.*;
import com.leo.hekima.to.*;
import com.leo.hekima.utils.*;
import io.r2dbc.postgresql.codec.Json;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
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
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.leo.hekima.service.UserService.getAuthentication;
import static com.leo.hekima.utils.ReactiveUtils.optionalEmptyDeferred;
import static com.leo.hekima.utils.ReactiveUtils.orEmptyList;
import static com.leo.hekima.utils.RequestUtils.getStringSet;
import static com.leo.hekima.utils.WebUtils.getPageAndSort;
import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.*;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.just;

@Component
public class NoteService {
    private static final Logger logger = LoggerFactory.getLogger(NoteService.class);
    private final NoteRepository noteRepository;
    private final NoteTagRepository noteTagRepository;
    private final TagRepository tagRepository;
    private final SourceRepository sourceRepository;
    private final File dataDir;
    private final String googleCredentialsPath;
    private ImageAnnotatorClient vision;
    private final WebClient webClient;
    private final WordAnalyzer wordAnalyzer;
    private final WordRepository wordRepository;
    private final NoteWordRepository noteWordRepository;
    public static final TypeReference<List<NoteFilePatchAction>> TR_LIST_OF_ACTIONS = new TypeReference<>() {};
    private final Map<String, NoteView> notesCache = new HashMap<>();
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public NoteService(NoteRepository noteRepository,
                       NoteTagRepository noteTagRepository, TagRepository tagRepository,
                       SourceRepository sourceRepository,
                       @Value("${google.credentials}") final String googleCredentialsPath,
                       @Value("${data.dir}") final String dataDirPath,
                       @Value("${subs.videoclipper.url}") final String videoClipperUrl, WordAnalyzer wordAnalyzer,
                       WordRepository wordRepository, NoteWordRepository noteWordRepository, R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.noteRepository = noteRepository;
        this.noteTagRepository = noteTagRepository;
        this.tagRepository = tagRepository;
        this.sourceRepository = sourceRepository;
        this.dataDir = new File(dataDirPath);
        this.googleCredentialsPath = googleCredentialsPath;
        this.webClient = WebClient.create(videoClipperUrl);
        this.wordAnalyzer = wordAnalyzer;
        this.wordRepository = wordRepository;
        this.noteWordRepository = noteWordRepository;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
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
        logger.info("Video clipper is at {}", videoClipperUrl);
        initVision();
    }

    @Transactional(readOnly = true)
    public Mono<ServerResponse> count(ServerRequest serverRequest) {
        final String baseRequest = """
            SELECT count(DISTINCT n.id) as notecount FROM note n
            LEFT JOIN note_tag nt ON n.id = nt.note_id
            LEFT JOIN tag t ON nt.tag_id = t.id
            LEFT JOIN note_source s ON n.source_id = s.id
            """;
        final Set<String> tags = getStringSet(serverRequest, "tags");
        final Set<String> notTags = getStringSet(serverRequest, "notTags");
        final Set<String> sources = getStringSet(serverRequest, "sources");
        final List<String> conditions = new ArrayList<>(3);
        if(isNotEmpty(tags)) {
            conditions.add(" t.uri IN ('" + String.join("','", tags) + "')");
        }
        if(isNotEmpty(notTags)) {
            conditions.add("""
                        n.id NOT IN (SELECT distinct(_nt.note_id) FROM note_tag _nt LEFT JOIN tag _t ON _nt.tag_id = _t.id
                                                            WHERE _t.uri IN ('""" + String.join("','", notTags) + "'))");
        }
        if(isNotEmpty(sources)) {
            conditions.add("s.uri IN ('" + String.join("','", sources) + "')");
        }
        final String request = baseRequest + (CollectionUtils.isEmpty(conditions) ? "" : " WHERE ") + String.join(" AND ", conditions);
        final Mono<Long> countNotes = r2dbcEntityTemplate.getDatabaseClient().sql(request)
                .map(row -> row.get("notecount", Long.class))
                .one()
                .switchIfEmpty(Mono.just(0L));
        return WebUtils.ok().body(countNotes, Long.class);
    }

    @Transactional(readOnly = true)
    public Mono<ServerResponse> search(ServerRequest request) {
        final List<String> conditions = new ArrayList<>();
        var pageAndSort = getPageAndSort(request);
        // TODO Sanitize
        request.queryParam("source")
            .filter(n -> !n.isBlank())
            .map(n -> n.toLowerCase(Locale.ROOT).trim())
            .filter(n -> n.matches("[a-z0-9]+"))
            .ifPresent(s -> conditions.add("note.source_id = (SELECT id FROM note_source WHERE uri = '" + s + "')"));
        request.queryParam("tags")
            .filter(StringUtils::isNotEmpty)
            .map(tu -> Arrays.stream(tu.split("\\s*,\\s*"))
                    .map(DataUtils::sanitize)
                    .filter(n -> n.matches("[a-z0-9]+"))
                    .collect(Collectors.toList()))
            .filter(l -> !l.isEmpty())
            .ifPresent(allTags -> conditions.add("note.id IN (SELECT note_id FROM note_tag LEFT JOIN tag on tag.id = note_tag.tag_id WHERE tag.uri in ('" + join("','", allTags) + "'))"));
        request.queryParam("notTags")
                .filter(StringUtils::isNotEmpty)
                .map(tu -> Arrays.stream(tu.split("\\s*,\\s*"))
                        .map(DataUtils::sanitize)
                        .filter(n -> n.matches("[a-z0-9]+"))
                        .collect(Collectors.toList()))
                .filter(l -> !l.isEmpty())
                .ifPresent(allTags -> conditions.add("note.id NOT IN (SELECT note_id FROM note_tag LEFT JOIN tag on tag.id = note_tag.tag_id WHERE tag.uri in ('" + join("','", allTags) + "'))"));
        final Set<Word> indexableWords = request.queryParam("q")
                .filter(StringUtils::isNotEmpty)
                .map(wordAnalyzer::getIndexableWords)
                .filter(l -> !l.isEmpty()).orElse(Collections.emptySet());
        if(!indexableWords.isEmpty()) {
            conditions.add("note.id IN (SELECT note_id FROM note_word LEFT JOIN word on word.id = note_word.word_id WHERE word.word in ('" + join("','", indexableWords.stream().map(Word::word).collect(Collectors.toSet())) + "'))");
        }
        final StringBuilder sql = new StringBuilder("SELECT id, uri, valeur, created_at, source_id, files, subs from note ");
        if(!conditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(join(" AND ", conditions));
        }
        sql.append(" ORDER BY created_at DESC OFFSET ");
        sql.append(pageAndSort.offset());
        sql.append(" LIMIT ");
        sql.append(pageAndSort.count());
        logger.debug("Start executing request");
        final long start = System.currentTimeMillis();
        var views = orEmptyList(r2dbcEntityTemplate.getDatabaseClient().sql(sql.toString()).fetch().all()
        .map(row -> new NoteModel(
            ((Integer) row.get("id")).longValue(),
            (String) row.get("uri"),
            (String) row.get("valeur"),
            ((OffsetDateTime) row.get("created_at")).toInstant(),
            Optional.ofNullable((Integer) row.get("source_id")).map(Integer::longValue).orElse(null),
            Optional.ofNullable(((Json) row.get("files")))
                .map(d -> JsonUtils.deserializeSilentFail(d.asString(), NoteFiles.class))
                .orElseGet(NoteFiles::new),
            Optional.ofNullable(((Json) row.get("subs")))
                    .map(d -> JsonUtils.deserializeSilentFail(d.asString(), NoteSubs.class))
                    .orElseGet(NoteSubs::new)
            )
        ))
        .flatMap(notes -> {
            var uris = notes.stream().map(NoteModel::getUri).toList();
            var transformers = notes.stream()
                .map(this::toView);
            if(!indexableWords.isEmpty()) {
                transformers = transformers.map(view -> highlight(view, indexableWords));
            }
            final var transformed = transformers.collect(Collectors.toList());
            return Mono.zip(transformed, aggregat -> Arrays.stream(aggregat).map(a -> (NoteView)a).collect(Collectors.toList()))
                .map(unorderdNoteViews -> {
                    final List<NoteView> sorted = new ArrayList<>(unorderdNoteViews);
                    sorted.sort(Comparator.comparingInt(h -> uris.indexOf(h.uri())));
                    return sorted;
                });
        }).switchIfEmpty(defer(() -> Mono.just(Collections.emptyList())));
        return ok().body(views, NoteView.class);
    }

    private Mono<NoteView> highlight(Mono<NoteView> noteViewMono, final Set<Word> highlights) {
        return noteViewMono.map(view -> {
            String highlighted = view.valeur();
            for (Word highlight : highlights) {
                highlighted = highlighted.replaceAll(highlight.word(), format("<span class='highlight'>%s</span>", highlight.word()));
            }
            return new NoteView(view.uri(), highlighted, view.tags(), view.source(), view.files(), view.subs());
        });
    }


    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return noteRepository.findByUri(uri)
            .doOnNext(note -> {
                notesCache.remove(uri);
                for (int i = 0; i < note.getFiles().files().size(); i++) {
                    this.deleteFile(note, i);
                }
            })
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
    public Mono<ServerResponse> patchFiles(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        notesCache.remove(uri);
        return noteRepository.findByUri(uri).zipWith(serverRequest.multipartData())
        .flatMap(tuple -> {
            final NoteModel note = tuple.getT1();
            final MultiValueMap<String, Part> d = tuple.getT2();
            return d.get("request").get(0).content()
            .collectList()
            .map(dataBuffers -> {
                byte[] bytes = new byte[dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum()];
                int start = 0;
                for (DataBuffer dataBuffer : dataBuffers) {
                    dataBuffer.read(bytes, start, dataBuffer.readableByteCount());
                    start += dataBuffer.readableByteCount();
                    DataBufferUtils.release(dataBuffer);
                }
                return JsonUtils.deserializeSilentFail(bytes,  TR_LIST_OF_ACTIONS);
            })
            .flatMap(actions -> executeActions(note, actions, d, 0, 0))
            .flatMap(finalNote -> WebUtils.ok().bodyValue(finalNote.getFiles()));
        });
    }

    private Mono<NoteModel> executeActions(final NoteModel note, final List<NoteFilePatchAction> actions,
                                           final MultiValueMap<String, Part> d, int actionIndex, int fileIndex) {
        final var action = actions.get(actionIndex);
        final Mono<Integer> mono;
        if(NoteFilePatchAction.DELETE.equals(action)) {
            if(!deleteFile(note, fileIndex)) {
                throw new UnrecoverableServiceException("Could not delete specified file " + fileIndex);
            }
            mono = Mono.just(0);
        } else if(NoteFilePatchAction.UPSERT.equals(action)) {
            final var currentfiles = note.getFiles().files();
            mono = upsertFile(note, currentfiles.size() > fileIndex ? currentfiles.get(fileIndex).fileId() : null, d.get("files").get(actionIndex))
            .map(whatever -> 1);
        } else {
            mono = Mono.just(1);
        }
        return mono.flatMap(deltaFileIndex -> noteRepository.save(note).then(defer(() -> {
            if (actionIndex >= actions.size() - 1) {
                return Mono.just(note);
            } else {
                return executeActions(note, actions, d, actionIndex + 1, fileIndex + deltaFileIndex);
            }
        })));
    }

    private Mono<NoteModel> upsertFile(final NoteModel note, final String oldFileId, final Part filePart) {
        notesCache.remove(note.getUri());
        final String mimeType = Objects.requireNonNull(filePart.headers().getContentType()).toString();
        final var noteFiles = note.getFiles();
        final var files = noteFiles.files();
        NoteFile nf = null;
        for (int i = 0; i < files.size(); i++) {
            final var file = files.get(i);
            if(file.fileId().equals(oldFileId)) {
                files.set(i, new NoteFile(mimeType, oldFileId));
                nf = file;
                break;
            }
        }
        if(nf == null) {
            final String newFileId = StringUtils.md5InHex(note.getUri() +  System.currentTimeMillis());
            nf = new NoteFile(mimeType, newFileId);
            noteFiles.files().add(nf);
        }
        final String newFileId = nf.fileId();
        logger.info("Writing file " + newFileId);
        final File newFile = getDataFile(newFileId);
        try {
            newFile.getParentFile().mkdirs();
            newFile.createNewFile();
        } catch (IOException e) {
            logger.error("Could not create file {}", newFile, e);
            throw new UnrecoverableServiceException("Cannot upload file");
        }
        return filePart.content().doOnNext(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            try {
                Files.write(newFile.toPath(), bytes, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UnrecoverableServiceException("Cannot write new file", e);
            }
        }).then(noteRepository.save(note));
    }

    private boolean deleteFile(NoteModel note, final int fileIndex) {
        final NoteFiles noteFiles = note.getFiles();
        final var files = noteFiles.files();
        final boolean deleted;
        if(files.size() > fileIndex) {
            logger.info("Renaming old file {} so it appears deleted", fileIndex);
            final var fileId = files.get(fileIndex).fileId();
            files.remove(fileIndex);
            deleted = true;
            final File oldFile = getDataFile(fileId);
            if(oldFile.exists()) {
                final boolean renameSuccess = oldFile.renameTo(getDataFile(fileId + "." + System.currentTimeMillis() + ".old"));
                if (!renameSuccess) {
                    logger.warn("Could not rename file {}", fileId);
                }
            } else {
                logger.warn("File {} does not exist anymore", fileId);
            }
        } else {
            deleted = false;
            logger.info("Cannot delete file {} for note {} as there is not so many files", fileIndex, note.getUri());
        }
        return deleted;
    }

    @Transactional(readOnly = true)
    public Mono<ServerResponse> getFile(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        final String fileId = serverRequest.pathVariable("fileId");
        return noteRepository.findByUri(uri)
            .filter(hekima -> hekima.getFiles() != null && hekima.getFiles().files() != null && hekima.getFiles().files().stream().anyMatch(f -> f.fileId().equals(fileId)))
            .map(hekima -> hekima.getFiles().files().stream().filter(f -> f.fileId().equals(fileId)).findAny().get())
            .flatMap(value -> {
                try {
                    return ok()
                        .contentType(MediaType.parseMediaType(value.mimeType()))
                        .body(BodyInserters.fromResource(new InputStreamResource(new FileInputStream(getDataFile(value.fileId())))));
                } catch (FileNotFoundException e) {
                    logger.error("Cannot read file {}", value.fileId());
                    throw new UnrecoverableServiceException("Cannot read file", e);
                }
            }).switchIfEmpty(notFound().build());
    }

    @Transactional
    public Mono<ServerResponse> upsert(ServerRequest serverRequest) {
        final var now = Instant.now();
        return serverRequest.body(toMono(NoteUpsertRequest.class))
            .flatMap(request ->  {
                final String uri = StringUtils.isNotEmpty(request.getUri()) ? request.getUri() : WebUtils.getOrCreateUri(request, serverRequest);
                notesCache.remove(uri);
                return Mono.zip(
                    Mono.just(request),
                    noteRepository.findByUri(uri).switchIfEmpty(defer(() -> Mono.just(new NoteModel(uri))))
                );
            })
            .flatMap(uriAndSource -> {
                NoteModel hekima = uriAndSource.getT2();
                final Mono<Tuple2<NoteUpsertRequest, NoteModel>> deletionTags;
                hekima.setSourceId(null);
                if(hekima.getId() == null) {
                    deletionTags = Mono.just(uriAndSource);
                } else {
                    deletionTags = Mono.zip(
                        noteRepository.deleteLinkWithTags(hekima.getId()),
                        noteRepository.deleteLinkWithWords(hekima.getId())
                    ).then(Mono.just(uriAndSource));
                }
                return deletionTags;
            })
            .flatMap(uriAndSource -> {
                final NoteUpsertRequest request = uriAndSource.getT1();
                NoteModel note = uriAndSource.getT2();
                note.setValeur(request.getValeur());
                if(note.getCreatedAt() == null) {
                    note.setCreatedAt(Instant.now());
                }
                if(isEmpty(request.getSubs())) {
                    note.setSubs(null);
                } else {
                    note.setSubs(new NoteSubs(request.getSubs().stream()
                    .map(s -> {
                        if(s.to() - s.from() < 2) {
                            return new NoteSub(s.name(), s.from() - 1, s.to() + 1);
                        } else {
                            return s;
                        }
                    })
                    .collect(Collectors.toList())));
                    for (NoteSub sub : note.getSubs().subs()) {
                        logger.debug("Asking to clip {} from {} to {}", sub.name(), sub.from(), sub.to());
                        var l = webClient.post()
                        .uri(b -> b.path("/api/clip")
                                .queryParam("name", sub.name())
                                .queryParam("from", sub.from())
                                .queryParam("to", sub.to())
                                  .build(sub.name(), sub.from(), sub.to()))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToMono(String.class).subscribe(b -> logger.debug("Clip call result : {}", b));
                    }
                }
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
                .flatMap(this::saveIndexedWords)
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


    @Transactional
    public Mono<ServerResponse> reindex(ServerRequest serverRequest) {
        notesCache.clear();
        return wordRepository.deleteAll()
        .thenMany(noteRepository.findAll())
        .flatMap(this::saveIndexedWords, 1)
        .then(ok().bodyValue(AckResponse.OK));
    }

    private Mono<NoteModel> saveIndexedWords(NoteModel savedNote) {
        final Set<Word> indexableWordsWithLanguage = wordAnalyzer.getIndexableWords(savedNote.getValeur());
        final Set<String> indexableWords = indexableWordsWithLanguage.stream()
                .map(Word::word)
                .map(w -> w.substring(0, Math.min(50, w.length()))).collect(Collectors.toSet());
        return Flux.fromIterable(indexableWords)
        .flatMap(word -> wordRepository.findByWord(word)
                .switchIfEmpty(defer(() -> just(new WordModel(word, Language.FRENCH)))))
        .collectList()
        .flatMap(words -> {
            final List<WordModel> toSave = words.stream().filter(w -> w.getId() == null).collect(Collectors.toList());
            if(toSave.isEmpty()){
                return just(words);
            } else {
                return wordRepository.saveAll(toSave).then(just(words));
            }
        })
        .flatMap(words -> noteWordRepository.saveAll(
                words.stream()
                    .map(w -> new NoteWordModel(savedNote.getId(), w.getId()))
                    .collect(Collectors.toList())).then())
        .then(defer(() -> just(savedNote)));
    }

    private File getDataFile(String fileId) {
        final String prefix1 = fileId.substring(0,2);
        final String prefix2 = fileId.substring(2,4);
        return new File(dataDir, prefix1 + "/" + prefix2 + "/" + fileId);
    }

    public Mono<NoteView> toView(NoteModel t) {
        final NoteView alreadyExistingNote = notesCache.get(t.getUri());
        if(alreadyExistingNote == null) {
            logger.debug("Cache miss for note {}", t.getUri());
            return Mono.zip(
                            orEmptyList(tagRepository.findByNoteId(t.getId())),
                            t.getSourceId() == null ?
                                    Mono.just(Optional.empty()) :
                                    sourceRepository.findById(t.getSourceId()).map(Optional::of).switchIfEmpty(Mono.just(Optional.empty())))
                    .map(tuple -> {
                        final var tags = TagService.toView(tuple.getT1());
                        final var source = tuple.getT2().map(s -> SourceService.toView((SourceModel) s)).orElse(null);
                        final NoteView noteView = new NoteView(t.getUri(), t.getValeur(), tags, source, t.getFiles().files(),
                            t.getSubs() == null ? null : t.getSubs().subs());
                        notesCache.put(t.getUri(), noteView);
                        return noteView;
                    });
        } else {
            logger.debug("Cache hit for note {}", t.getUri());
            return Mono.just(alreadyExistingNote);
        }
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

    public Mono<ServerResponse> autoCompleteIndex(ServerRequest serverRequest) {
        final int count = RequestUtils.getCount(serverRequest);
        final int offset = RequestUtils.getOffset(serverRequest);
        return serverRequest.queryParam("q")
        .map(String::trim)
        .filter(StringUtils::isNotEmpty)
        .map(q ->
            WebUtils.ok().body(wordRepository.findByWordLikeOrderedByLengthAsc(q.toLowerCase(Locale.FRENCH) + '%')
                .skip(offset)
                .take(count)
                .collectList(), List.class)
        ).orElseGet(() -> WebUtils.ok().bodyValue(new ArrayList<String>(0)));
    }
}
