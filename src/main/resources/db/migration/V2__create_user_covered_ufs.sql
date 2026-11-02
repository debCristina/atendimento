CREATE TABLE user_covered_ufs (
    user_id BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    uf      VARCHAR(2) NOT NULL,
    PRIMARY KEY (user_id, uf)
);