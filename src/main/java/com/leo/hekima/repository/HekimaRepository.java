package com.leo.hekima.repository;


import com.leo.hekima.model.HekimaModel;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface HekimaRepository extends ReactiveNeo4jRepository<HekimaModel, String> {
}
