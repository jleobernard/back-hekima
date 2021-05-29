CREATE USER notes WITH PASSWORD '';
create database notes with owner = notes ENCODING = 'UTF8';
CREATE SCHEMA AUTHORIZATION notes;
create table notes.tag (
id SERIAL PRIMARY KEY,
uri VARCHAR(150) UNIQUE NOT NULL,
valeur VARCHAR(150) UNIQUE NOT NULL,
valeur_recherche VARCHAR(150) NOT NULL,
last_used TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

create table notes.note_source (
id SERIAL PRIMARY KEY,
uri VARCHAR(150) UNIQUE NOT NULL,
titre VARCHAR(150) UNIQUE NOT NULL,
titre_recherche VARCHAR(150) NOT NULL,
auteur VARCHAR(150) DEFAULT NULL,
source_type VARCHAR(25) NOT NULL,
last_used TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

create table notes.note (
id SERIAL PRIMARY KEY,
uri VARCHAR(150) UNIQUE NOT NULL,
valeur TEXT NOT NULL,
mime_type VARCHAR(25) DEFAULT NULL,
file_id VARCHAR(150) DEFAULT NULL,
source_id int DEFAULT NULL,
created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_note_source
      FOREIGN KEY(source_id)
      REFERENCES note_source(id)
      ON DELETE CASCADE
);
CREATE INDEX note_source_id ON note USING btree (source_id);

create table notes.note_tag (
id SERIAL PRIMARY KEY,
note_id int NOT NULL,
tag_id int not null,
UNIQUE (note_id, tag_id),
CONSTRAINT fk_note_tag_note
      FOREIGN KEY(note_id)
      REFERENCES note(id)
      ON DELETE CASCADE,
CONSTRAINT fk_note_tag_tag
      FOREIGN KEY(tag_id)
      REFERENCES tag(id)
      ON DELETE CASCADE
);
CREATE INDEX note_tag_note_id ON note_tag USING btree (note_id);
CREATE INDEX note_tag_tag_id ON note_tag USING btree (tag_id);

CREATE TABLE notes.user_account (
    id SERIAL PRIMARY KEY,
    uri VARCHAR(150) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255)
);
