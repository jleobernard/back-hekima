package com.leo.hekima.handler;

import com.leo.hekima.model.HekimaSourceModel;
import com.leo.hekima.model.HekimaTagModel;
import com.leo.hekima.repository.SourceRepository;
import com.leo.hekima.utils.RequestUtils;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.leo.hekima.utils.DatatUtils.sanitize;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.parameter;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class TagHandler {
    private final ReactiveNeo4jTemplate neo4jTemplate;
    private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();

    public TagHandler(ReactiveNeo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate;
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
        Flux<HekimaTagModel> sources = neo4jTemplate.findAll(cypher, parameters, HekimaTagModel.class);
        return ok().contentType(MediaType.APPLICATION_JSON).body(sources, HekimaTagModel.class);
    }
}
