CREATE TABLE solicitations (
    id               BIGSERIAL     PRIMARY KEY,
    client_id        BIGINT        NOT NULL REFERENCES users (id),
    status           VARCHAR(20)   NOT NULL,
    current_step     INTEGER      NOT NULL,

    service_type     VARCHAR(20)   NOT NULL,
    title            VARCHAR(80)   NOT NULL,
    description      VARCHAR(1000) NOT NULL,

    cep              VARCHAR(8),
    street           VARCHAR(255),
    neighborhood     VARCHAR(255),
    city             VARCHAR(255),
    uf               VARCHAR(2),
    number           VARCHAR(20),
    complement       VARCHAR(255),

    priority         VARCHAR(10),
    preferred_date   DATE,
    estimated_value  NUMERIC(12, 2),
    terms_accepted   BOOLEAN       NOT NULL DEFAULT FALSE,

    analyst_id       BIGINT        REFERENCES users (id),
    decision_comment VARCHAR(1000),

    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    submitted_at     TIMESTAMPTZ,
    decided_at       TIMESTAMPTZ,

    CONSTRAINT ck_solicitations_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_solicitations_step
        CHECK (current_step BETWEEN 1 AND 3)
);

CREATE INDEX idx_solicitations_client ON solicitations (client_id);
CREATE INDEX idx_solicitations_status_uf ON solicitations (status, uf);