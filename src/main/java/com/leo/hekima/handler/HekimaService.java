package com.leo.hekima.handler;

import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.model.HekimaModel;
import com.leo.hekima.model.HekimaSourceModel;
import com.leo.hekima.model.HekimaTagModel;
import com.leo.hekima.repository.HekimaRepository;
import com.leo.hekima.repository.SourceRepository;
import com.leo.hekima.repository.TagRepository;
import com.leo.hekima.to.HekimaUpsertRequest;
import com.leo.hekima.to.HekimaView;
import com.leo.hekima.utils.RequestUtils;
import com.leo.hekima.utils.StringUtils;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.cypherdsl.core.Cypher.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class HekimaService {
    private static final Logger logger = LoggerFactory.getLogger(HekimaService.class);
    private final HekimaRepository hekimaRepository;
    private final TagRepository tagRepository;
    private final SourceRepository sourceRepository;
    private final ReactiveNeo4jTemplate neo4jTemplate;
    private final File dataDir;
    private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();

    public HekimaService(HekimaRepository hekimaRepository,
                         TagRepository tagRepository,
                         SourceRepository sourceRepository,
                         ReactiveNeo4jTemplate neo4jTemplate,
                         @Value("${data.dir}") final String dataDirPath) {
        this.hekimaRepository = hekimaRepository;
        this.tagRepository = tagRepository;
        this.sourceRepository = sourceRepository;
        this.neo4jTemplate = neo4jTemplate;
        this.dataDir = new File(dataDirPath);
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
    }

    @Transactional
    public Mono<ServerResponse> search(ServerRequest request) {
        Map<String, Object> parameters = new HashMap<>();
        Node m = node("Hekima").named("m");
        Node s = anyNode("s");
        Node t = anyNode("t");
        Relationship rs = m.relationshipTo(s, "SOURCE");
        Relationship rt = m.relationshipTo(t, "TAG");
        StatementBuilder.OngoingReadingWithoutWhere filter = Cypher.match(m);
        request.queryParam("source").ifPresent(sourceUri -> {
            filter.match(rs);
            s.withProperties("uri", parameter("source"));
            parameters.put("source", sourceUri);
        });
        List<String> tags = request.queryParams().get("tag");
        if(!isEmpty(tags)) {
            filter.match(rt);
            filter.where(t.property("uri").in(parameter("tags")));
            parameters.put("tags", tags);
        }
        StatementBuilder.BuildableStatement statement = filter
                .returningDistinct(m)
                .orderBy(m.property("createdAt").descending())
                .skip(RequestUtils.getOffset(request))
                .limit(RequestUtils.getCount(request));
        String cypher = cypherRenderer.render(statement.build());
        Mono<Object> hekimas = neo4jTemplate.findAll(cypher, parameters, HekimaModel.class)
            .map(HekimaModel::getUri)
            .collectList()
            .flatMap(uris -> hekimaRepository.findAllById(uris).collectList()
                .map(models -> {
                    final List<HekimaModel> sorted = new ArrayList<>(models);
                    sorted.sort(Comparator.comparingInt(h -> uris.indexOf(h.getUri())));
                    return sorted.stream().map(HekimaService::toView).collect(Collectors.toList());
                }));
        return ok().contentType(MediaType.APPLICATION_JSON).body(hekimas, HekimaView.class);
    }


    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return hekimaRepository.findById(uri)
            .doOnNext(this::deleteFile)
            .flatMap(hekimaRepository::delete)
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
        return hekimaRepository.findById(uri)
            .map(HekimaService::toView)
            .flatMap(value -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(value)));
    }

    @Transactional
    public Mono<ServerResponse> uploadFile(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return hekimaRepository.findById(uri).zipWith(serverRequest.multipartData())
            .flatMap(tuple -> {
                final HekimaModel hekima = tuple.getT1();
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
                }).then(hekimaRepository.save(hekima));
            })
            .flatMap(value -> ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(toView(value))));
    }

    @Transactional
    public Mono<ServerResponse> deleteFile(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return hekimaRepository.findById(uri).zipWith(serverRequest.multipartData())
            .flatMap(tuple -> {
                final HekimaModel hekima = tuple.getT1();
                final MultiValueMap<String, Part> d = tuple.getT2();
                deleteFile(hekima);
                return hekimaRepository.save(hekima);
            })
            .flatMap(value -> ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(toView(value))));
    }

    private void deleteFile(HekimaModel hekima) {
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
        return hekimaRepository.findById(uri)
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
        return serverRequest.body(toMono(HekimaUpsertRequest.class))
            .flatMap(request ->  {
                final String uri = request.getUri() == null ?
                    StringUtils.md5InHex(request.getValeur() +"#"+request.getSource() + "#" + String.join("-",request.getTags())) :
                    request.getUri();
                return Mono.zip(
                    Mono.just(request),
                    hekimaRepository.findById(uri).switchIfEmpty(Mono.defer(() -> Mono.just(new HekimaModel(uri))))
                );
            })
            .doOnNext(uriAndSource -> {
                HekimaModel hekima = uriAndSource.getT2();
                hekimaRepository.deleteSourceAndTags(hekima.getUri());
                if(hekima.getTags() == null) {
                    hekima.setTags(new ArrayList<>());
                } else {
                    hekima.getTags().clear();
                }
            })
            .flatMap(uriAndSource -> {
                final HekimaUpsertRequest request = uriAndSource.getT1();
                HekimaModel hekima = uriAndSource.getT2();
                hekima.setValeur(request.getValeur());
                hekima.setCreatedAt(System.currentTimeMillis());
                Flux<HekimaTagModel> tagsFlux = (isEmpty(request.getTags()) ? Flux.empty() : tagRepository.findAllById(request.getTags()));
                tagsFlux.doOnNext(tag -> hekima.getTags().add(tag))
                        .subscribe();
                Mono<HekimaSourceModel> sourceMono =
                        request.getSource() == null ? Mono.empty() : sourceRepository.findById(request.getSource());
                sourceMono.subscribe(hekima::setSource);
                final Mono<HekimaModel> finalFlux = Flux.zip(tagsFlux, sourceMono.flux())
                        .then(hekimaRepository.save(hekima));
                finalFlux.subscribe();
                return finalFlux;
            }).flatMap(savedTag -> ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(toView(savedTag))));
    }

    private File getDataFile(String fileId) {
        final String prefix1 = fileId.substring(0,2);
        final String prefix2 = fileId.substring(2,4);
        return new File(dataDir, prefix1 + "/" + prefix2 + "/" + fileId);
    }

    public static HekimaView toView(HekimaModel t) {
        return new HekimaView(t.getUri(), t.getValeur(), t.getCreatedAt(), TagService.toView(t.getTags()), SourceService.toView(t.getSource()), hasText(t.getFileId()));
    }
}
