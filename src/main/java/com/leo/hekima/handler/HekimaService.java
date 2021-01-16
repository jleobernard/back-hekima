package com.leo.hekima.handler;

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
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.cypherdsl.core.Cypher.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class HekimaService {
    private final HekimaRepository hekimaRepository;
    private final TagRepository tagRepository;
    private final SourceRepository sourceRepository;
    private final ReactiveNeo4jTemplate neo4jTemplate;
    private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();

    public HekimaService(HekimaRepository hekimaRepository, TagRepository tagRepository, SourceRepository sourceRepository, ReactiveNeo4jTemplate neo4jTemplate) {
        this.hekimaRepository = hekimaRepository;
        this.tagRepository = tagRepository;
        this.sourceRepository = sourceRepository;
        this.neo4jTemplate = neo4jTemplate;
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
        Flux<HekimaView> hekimas = neo4jTemplate.findAll(cypher, parameters, HekimaModel.class)
            .flatMap(uri -> hekimaRepository.findById(uri.getUri()))
            .map(HekimaService::toView);
        return ok().contentType(MediaType.APPLICATION_JSON).body(hekimas, HekimaView.class);
    }


    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return hekimaRepository.deleteById(uri)
                .flatMap(value -> noContent().build())
                .onErrorStop().flatMap(err -> status(HttpStatus.INTERNAL_SERVER_ERROR).build());
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
            .doOnNext(uriAndSource -> hekimaRepository.deleteSourceAndTags(uriAndSource.getT2().getUri()))
            .flatMap(uriAndSource -> {
                final HekimaUpsertRequest request = uriAndSource.getT1();
                HekimaModel hekima = uriAndSource.getT2();
                hekima.setValeur(request.getValeur());
                hekima.setCreatedAt(System.currentTimeMillis());
                if(hekima.getTags() == null) {
                    hekima.setTags(new ArrayList<>());
                } else {
                    hekima.getTags().clear();
                }
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

    public static HekimaView toView(HekimaModel t) {
        return new HekimaView(t.getUri(), t.getValeur(), t.getCreatedAt(), TagService.toView(t.getTags()), SourceService.toView(t.getSource()));
    }
}
