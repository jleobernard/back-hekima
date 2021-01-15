package com.leo.hekima.repository;

import com.leo.hekima.model.HekimaTagModel;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface TagRepository extends ReactiveNeo4jRepository<HekimaTagModel, String> {
}
