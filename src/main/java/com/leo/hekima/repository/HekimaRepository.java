package com.leo.hekima.repository;


import com.leo.hekima.model.HekimaModel;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import reactor.core.publisher.Mono;

public interface HekimaRepository extends ReactiveNeo4jRepository<HekimaModel, String> {
    @Query("MATCH (h:Hekima{uri:{uri}})-[r:TAG|:SOURCE]->(tagOrSource) DELETE r")
    Mono<Void> deleteSourceAndTags(String uri);
}
