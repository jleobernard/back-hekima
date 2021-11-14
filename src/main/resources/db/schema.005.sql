create table notes.note_quizz_histo (
id SERIAL PRIMARY KEY,
note_id int NOT NULL,
score float NOT NULL,
created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_note_quizz_histo_note_id
      FOREIGN KEY(note_id)
      REFERENCES note(id)
      ON DELETE CASCADE
);
CREATE INDEX note_quizz_histo_note_id ON note_quizz_histo USING btree (note_id);