create table notes.word (
id SERIAL PRIMARY KEY,
word varchar(25) NOT NULL,
language int NOT NULL);

create table notes.note_word (
id SERIAL PRIMARY KEY,
note_id int NOT NULL,
word_id int NOT NULL,
CONSTRAINT fk_note_word_note_id
      FOREIGN KEY(note_id)
      REFERENCES note(id)
      ON DELETE CASCADE,
CONSTRAINT fk_note_word_word_id
      FOREIGN KEY(word_id)
      REFERENCES word(id)
      ON DELETE CASCADE
);
CREATE INDEX note_word_note_id ON note_word USING btree (note_id);
CREATE INDEX note_word_word_id ON note_word USING btree (word_id);