package com.leo.hekima.handler;

import com.leo.hekima.model.SourceModel;
import com.leo.hekima.repository.SourceRepository;
import com.leo.hekima.to.SourceUpsertRequest;
import com.leo.hekima.to.SourceView;
import com.leo.hekima.utils.DataUtils;
import com.leo.hekima.utils.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static com.leo.hekima.utils.DataUtils.sanitize;
import static com.leo.hekima.utils.WebUtils.getPageAndSort;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class SourceService {
    private final SourceRepository sourceRepository;
    private final R2dbcEntityTemplate template;

    public SourceService(SourceRepository sourceRepository,
                         R2dbcEntityTemplate template) {
        this.sourceRepository = sourceRepository;
        this.template = template;
    }

    public Mono<ServerResponse> search(ServerRequest request) {
        var pageAndSort = getPageAndSort(request);
        var c = Criteria.empty();
        c = c.and(request.queryParam("q")
                .filter(n -> !n.isBlank())
                .map(q -> where("titre_recherche").like('%' + sanitize(q) + '%'))
                .orElseGet(Criteria::empty));
        final Query query = Query.query(CriteriaDefinition.from(c))
                .offset(pageAndSort.offset()).limit(pageAndSort.count())
                .sort(Sort.by(Sort.Direction.DESC, "last_used"));
        final Flux<SourceView> sources = template.select(SourceModel.class)
                .matching(query).all()
                .map(SourceService::toView);
        return ok().contentType(MediaType.APPLICATION_JSON).body(sources, SourceView.class);
    }

    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return sourceRepository.deleteByUri(uri)
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
                        sourceRepository.findByUri(uri).switchIfEmpty(Mono.defer(() -> {
                            final SourceModel newTag = new SourceModel();
                            newTag.setUri(uri);
                            return Mono.just(newTag);
                        })));
            })
            .flatMap(uriAndSource -> {
                final SourceUpsertRequest request = uriAndSource.getT1();
                SourceModel source = uriAndSource.getT2();
                source.setAuteur(request.getAuteur());
                source.setTitre(request.getTitre());
                source.setTitreRecherche(DataUtils.sanitize(request.getTitre()));
                source.setType(request.getType());
                source.setLastUsed(Instant.now());
                return sourceRepository.save(source);
            }).flatMap(savedTag -> ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(toView(savedTag))));
    }

    public static SourceView toView(SourceModel t) {
        if(t == null) {
            return null;
        }
        return new SourceView(t.getUri(), t.getTitre(), t.getTitreRecherche(), t.getAuteur(), t.getType());
    }
}
