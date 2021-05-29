package com.leo.hekima.repository;

import com.leo.hekima.model.NoteTagModel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface NoteTagRepository extends ReactiveCrudRepository<NoteTagModel, Long> {

}
