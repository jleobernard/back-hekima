package com.leo.hekima.handler;

import com.leo.hekima.model.HekimaSourceModel;
import com.leo.hekima.repository.SourceRepository;
import com.leo.hekima.to.SourceUpsertRequest;
import com.leo.hekima.to.SourceView;
import com.leo.hekima.utils.DataUtils;
import com.leo.hekima.utils.RequestUtils;
import com.leo.hekima.utils.StringUtils;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
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

import java.util.HashMap;
import java.util.Map;

import static com.leo.hekima.utils.DataUtils.sanitize;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.parameter;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class SourceService {
    private final SourceRepository sourceRepository;
    private final ReactiveNeo4jTemplate neo4jTemplate;
    private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();

    public SourceService(SourceRepository sourceRepository, ReactiveNeo4jTemplate neo4jTemplate) {
        this.sourceRepository = sourceRepository;
        this.neo4jTemplate = neo4jTemplate;
    }

    public Mono<ServerResponse> search(ServerRequest request) {
        Map<String, Object> parameters = new HashMap<>();
        Node m = node("Source").named("s");
        StatementBuilder.OngoingReadingWithoutWhere filter = Cypher.match(m);
        request.queryParam("q").ifPresent(q -> {
            filter.where(m.property("titreRecherche").contains(parameter("q")));
            parameters.put("q", sanitize(q));
        });
        StatementBuilder.BuildableStatement statement = filter
                .returning(m)
                .orderBy(m.property("lastUsed").descending())
                .skip(RequestUtils.getOffset(request))
                .limit(RequestUtils.getCount(request));
        String cypher = cypherRenderer.render(statement.build());
        Flux<SourceView> sources = neo4jTemplate.findAll(cypher, parameters, HekimaSourceModel.class)
                .map(SourceService::toView);
        return ok().contentType(MediaType.APPLICATION_JSON).body(sources, SourceView.class);
    }

    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return sourceRepository.deleteById(uri)
            .flatMap(value -> noContent().build())
                .onErrorStop().flatMap(err -> status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @Transactional
    public Mono<ServerResponse> upsert(ServerRequest serverRequest) {
        return serverRequest.body(toMono(SourceUpsertRequest.class))
                .flatMap(request ->  {
                    final String uri = StringUtils.md5InHex(request.getAuteur() +"#"+request.getTitre() + "#" + request.getType());
                    return Mono.zip(
                            Mono.just(request),
                            sourceRepository.findById(uri).switchIfEmpty(Mono.defer(() -> {
                                final HekimaSourceModel newTag = new HekimaSourceModel();
                                newTag.setUri(uri);
                                return Mono.just(newTag);
                            })));
                })
                .flatMap(uriAndSource -> {
                    final SourceUpsertRequest request = uriAndSource.getT1();
                    HekimaSourceModel source = uriAndSource.getT2();
                    source.setAuteur(request.getAuteur());
                    source.setTitre(request.getTitre());
                    source.setTitreRecherche(DataUtils.sanitize(request.getTitre()));
                    source.setType(request.getType());
                    source.setLastUsed(System.currentTimeMillis());
                    return sourceRepository.save(source);
                }).flatMap(savedTag -> ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(toView(savedTag))));
    }

    public static SourceView toView(HekimaSourceModel t) {
        if(t == null) {
            return null;
        }
        return new SourceView(t.getUri(), t.getTitre(), t.getTitreRecherche(), t.getAuteur(), t.getType(), t.getLastUsed());
    }
}
