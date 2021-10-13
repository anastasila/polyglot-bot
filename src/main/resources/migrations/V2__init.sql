CREATE TABLE subscriptions
(
    id            BIGSERIAL PRIMARY KEY,
    chat_id       BIGINT    NOT NULL UNIQUE,
    is_active     BOOLEAN   NOT NULL DEFAULT TRUE,
    interval      BIGINT    NOT NULL DEFAULT 21600,
    last_notified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
