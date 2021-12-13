package com.leo.hekima.repository;


import com.leo.hekima.model.NoteWordModel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface NoteWordRepository extends ReactiveCrudRepository<NoteWordModel, Long> {

}
