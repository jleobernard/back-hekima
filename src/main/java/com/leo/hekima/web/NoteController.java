package com.leo.hekima.web;

import com.leo.hekima.service.NoteService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoteController {
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }
}
