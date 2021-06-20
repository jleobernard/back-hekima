ALTER table notes.note ADD COLUMN files jsonb default NULL;



-- EXECUTE the following only after data migration
ALTER table notes.note DROP COLUMN mime_type;
ALTER table notes.note DROP COLUMN file_id;
