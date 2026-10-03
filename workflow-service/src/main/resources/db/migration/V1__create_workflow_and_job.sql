CREATE TABLE workflow (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_workflow_name UNIQUE (name)
);

CREATE TABLE job (
    id             VARCHAR(36)  PRIMARY KEY,
    workflow_id    BIGINT       NOT NULL REFERENCES workflow (id),
    workflow_name  VARCHAR(255) NOT NULL,
    payload        TEXT,
    status         VARCHAR(32)  NOT NULL,
    error          TEXT,
    enqueued_at    TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_job_status ON job (status);
CREATE INDEX idx_job_enqueued_at ON job (enqueued_at);
CREATE INDEX idx_job_workflow_id ON job (workflow_id);
