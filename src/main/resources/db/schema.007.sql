create table notes.refresh_token (
id SERIAL PRIMARY KEY,
user_id int NOT NULL,
token varchar(36) NOT NULL,
CONSTRAINT fk_refresh_token_user_id
      FOREIGN KEY(user_id)
      REFERENCES user_account(id)
      ON DELETE CASCADE);

CREATE INDEX refresh_token_user_id ON refresh_token USING btree (user_id);
CREATE INDEX refresh_token_token ON refresh_token USING btree (token);