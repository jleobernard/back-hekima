package com.leo.hekima.handler;

import com.leo.hekima.model.HekimaTagModel;
import com.leo.hekima.repository.TagRepository;
import com.leo.hekima.to.TagUpserRequest;
import com.leo.hekima.to.TagView;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.leo.hekima.utils.DataUtils.sanitize;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.parameter;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Service
public class TagService {
    private final ReactiveNeo4jTemplate neo4jTemplate;
    private final TagRepository tagRepository;
    private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();

    public TagService(ReactiveNeo4jTemplate neo4jTemplate, TagRepository tagRepository) {
        this.neo4jTemplate = neo4jTemplate;
        this.tagRepository = tagRepository;
    }

    public static List<TagView> toView(List<HekimaTagModel> tags) {
        if( tags == null) {
            return null;
        } else if (tags.isEmpty()) {
            return Collections.emptyList();
        } else {
            return tags.stream().map(TagService::toView).collect(Collectors.toList());
        }
    }

    public Mono<ServerResponse> search(ServerRequest request) {
        Map<String, Object> parameters = new HashMap<>();
        Node m = node("Tag").named("s");
        StatementBuilder.OngoingReadingWithoutWhere filter = Cypher.match(m);
        request.queryParam("q").ifPresent(q -> {
            filter.where(m.property("valeurRecherche").contains(parameter("q")));
            parameters.put("q", sanitize(q));
        });
        StatementBuilder.BuildableStatement statement = filter
                .returning(m)
                .orderBy(m.property("lastUsed").descending())
                .skip(RequestUtils.getOffset(request))
                .limit(RequestUtils.getCount(request));
        String cypher = cypherRenderer.render(statement.build());
        Flux<TagView> sources = neo4jTemplate.findAll(cypher, parameters, HekimaTagModel.class)
                .map(TagService::toView);
        return ok().contentType(MediaType.APPLICATION_JSON).body(sources, TagView.class);
    }

    @Transactional
    public Mono<ServerResponse> upsert(ServerRequest serverRequest) {
        return serverRequest.body(toMono(TagUpserRequest.class))
            .flatMap(request ->  {
                final String uri = StringUtils.md5InHex(request.getValeur());
                return Mono.zip(
                    Mono.just(request),
                    tagRepository.findById(uri).switchIfEmpty(Mono.defer(() -> {
                        final HekimaTagModel newTag = new HekimaTagModel();
                        newTag.setUri(uri);
                        return Mono.just(newTag);
                    })));
            })
            .flatMap(uriAndTag -> {
                final TagUpserRequest request = uriAndTag.getT1();
                HekimaTagModel tag = uriAndTag.getT2();
                tag.setValeur(request.getValeur());
                tag.setValeurRecherche(DataUtils.sanitize(request.getValeur()));
                tag.setLastUsed(System.currentTimeMillis());
                return tagRepository.save(tag);
            }).flatMap(savedTag -> ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(toView(savedTag))));
    }

    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return tagRepository.deleteById(uri)
                .flatMap(value -> noContent().build());
    }

    public static TagView toView(HekimaTagModel t) {
        return new TagView(t.getUri(), t.getValeur(), t.getValeurRecherche(), null, t.getLastUsed());
    }
}
