package com.leo.hekima.handler;

import com.leo.hekima.model.TagModel;
import com.leo.hekima.repository.TagRepository;
import com.leo.hekima.to.TagUpserRequest;
import com.leo.hekima.to.TagView;
import com.leo.hekima.utils.DataUtils;
import com.leo.hekima.utils.WebUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.leo.hekima.utils.DataUtils.sanitize;
import static com.leo.hekima.utils.WebUtils.getPageAndSort;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.ServerResponse.noContent;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Service
public class TagService {
    private final R2dbcEntityTemplate template;
    private final TagRepository tagRepository;

    public TagService(R2dbcEntityTemplate template, TagRepository tagRepository) {
        this.template = template;
        this.tagRepository = tagRepository;
    }

    public static List<TagView> toView(List<TagModel> tags) {
        if( tags == null) {
            return null;
        } else if (tags.isEmpty()) {
            return Collections.emptyList();
        } else {
            return tags.stream().map(TagService::toView).collect(Collectors.toList());
        }
    }

    public Mono<ServerResponse> search(ServerRequest request) {
        var pageAndSort = getPageAndSort(request);
        var c = Criteria.empty();
        c = c.and(request.queryParam("q")
                .filter(n -> !n.isBlank())
                .map(q -> where("valeur_recherche").like('%' + sanitize(q) + '%'))
                .orElseGet(Criteria::empty));
        final Query query = Query.query(CriteriaDefinition.from(c))
                .offset(pageAndSort.offset()).limit(pageAndSort.count())
                .sort(Sort.by(Sort.Direction.DESC, "last_used"));
        final Flux<TagView> sources = template.select(TagModel.class)
                .matching(query).all()
                .map(TagService::toView);
        return ok().contentType(MediaType.APPLICATION_JSON).body(sources, TagView.class);
    }

    @Transactional
    public Mono<ServerResponse> upsert(ServerRequest serverRequest) {
        return serverRequest.body(toMono(TagUpserRequest.class))
            .flatMap(request ->  {
                final String uri = WebUtils.getOrCreateUri(request, serverRequest);
                return Mono.zip(
                    Mono.just(request),
                    tagRepository.findByUri(uri).switchIfEmpty(Mono.defer(() -> {
                        final TagModel newTag = new TagModel();
                        newTag.setUri(uri);
                        return Mono.just(newTag);
                    })));
            })
            .flatMap(uriAndTag -> {
                final TagUpserRequest request = uriAndTag.getT1();
                TagModel tag = uriAndTag.getT2();
                tag.setValeur(request.getValeur());
                tag.setValeurRecherche(DataUtils.sanitize(request.getValeur()));
                tag.setLastUsed(Instant.now());
                return tagRepository.save(tag);
            }).flatMap(savedTag -> ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(toView(savedTag))));
    }

    @Transactional
    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        final String uri = serverRequest.pathVariable("uri");
        return tagRepository.deleteByUri(uri)
                .flatMap(value -> noContent().build());
    }

    public static TagView toView(TagModel t) {
        return new TagView(t.getUri(), t.getValeur(), t.getValeurRecherche(), null);
    }
}
