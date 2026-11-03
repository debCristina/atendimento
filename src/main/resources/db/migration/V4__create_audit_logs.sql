CREATE TABLE audit_logs (
    id           BIGSERIAL    PRIMARY KEY,
    action       VARCHAR(50)  NOT NULL,
    user_id      BIGINT,
    user_role    VARCHAR(20),
    resource_id  VARCHAR(50),
    outcome      VARCHAR(10)  NOT NULL,
    reason       VARCHAR(500),
    duration_ms  BIGINT       NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_audit_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);