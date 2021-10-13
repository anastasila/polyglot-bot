CREATE TABLE dictionary
(
    id      BIGSERIAL PRIMARY KEY,
    name    TEXT   NOT NULL,
    chat_id BIGINT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (chat_id, name)
);

CREATE TABLE words
(
    id            BIGSERIAL PRIMARY KEY,
    chat_id       BIGINT    NOT NULL,
    dictionary_id BIGINT    NOT NULL REFERENCES dictionary (id),
    word          TEXT      NOT NULL,
    definition    TEXT      NOT NULL,
    example       TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (chat_id, word)
);
