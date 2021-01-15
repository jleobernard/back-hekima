package com.leo.hekima.handler;

import com.leo.hekima.model.HekimaModel;
import com.leo.hekima.repository.HekimaRepository;
import com.leo.hekima.repository.SourceRepository;
import com.leo.hekima.repository.TagRepository;
import com.leo.hekima.utils.RequestUtils;
import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.cypherdsl.core.Cypher.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class HekimaHandler {
    private final HekimaRepository hekimaRepository;
    private final TagRepository tagRepository;
    private final SourceRepository sourceRepository;
    private final ReactiveNeo4jTemplate neo4jTemplate;
    private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();

    public HekimaHandler(HekimaRepository hekimaRepository, TagRepository tagRepository, SourceRepository sourceRepository, ReactiveNeo4jTemplate neo4jTemplate) {
        this.hekimaRepository = hekimaRepository;
        this.tagRepository = tagRepository;
        this.sourceRepository = sourceRepository;
        this.neo4jTemplate = neo4jTemplate;
    }

    //@Transactional("reactiveTransactionManager")
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
        Flux<HekimaModel> hekimas = neo4jTemplate.findAll(cypher, parameters, HekimaModel.class)
                .flatMap(uri -> hekimaRepository.findById(uri.getUri()));
        return ok().contentType(MediaType.APPLICATION_JSON).body(hekimas, HekimaModel.class);
    }
}
