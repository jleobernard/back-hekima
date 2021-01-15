package com.leo.hekima.repository;

import com.leo.hekima.model.HekimaSourceModel;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface SourceRepository extends ReactiveNeo4jRepository<HekimaSourceModel, String> {
}
