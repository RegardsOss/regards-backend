---------------------
-- BATCHES
CREATE TABLE t_batch (
    id                  uuid         NOT NULL,
    process_business_id uuid         NOT NULL,
    tenant              varchar(255) NOT NULL,
    user_email          varchar(255) NOT NULL,
    user_role           varchar(255) NOT NULL,
    correlation_id      text         NOT NULL,
    filesets            jsonb        NOT NULL,
    parameters          jsonb        NOT NULL,
    primary key (id)
);

CREATE INDEX ON t_batch (tenant);
CREATE INDEX ON t_batch (user_role);
CREATE INDEX ON t_batch (process_business_id);

---------------------
-- EXECUTIONS
CREATE TABLE t_execution (
    timeout_after_millis int8         NOT NULL,
    version              int8         NOT NULL,
    created              timestamptz  NOT NULL,
    last_updated         timestamptz  NOT NULL,
    id                   uuid         NOT NULL,
    batch_id             uuid         REFERENCES t_batch ON DELETE CASCADE,
    process_business_id  uuid         NOT NULL,
    current_status       varchar(10)  NOT NULL,
    tenant               varchar(255) NOT NULL,
    user_email           varchar(255) NOT NULL,
    correlation_id       text         NOT NULL,
    batch_correlation_id text         NOT NULL,
    steps                jsonb        NOT NULL,
    file_parameters      jsonb        NOT NULL,
    primary key (id)
);

CREATE INDEX ON t_execution (last_updated);
CREATE INDEX ON t_execution (current_status);
CREATE INDEX ON t_execution (user_email);
CREATE INDEX ON t_execution (tenant);

---------------------
-- OUTPUT FILES
CREATE TABLE t_outputfile (
    downloaded            boolean     NOT NULL,
    deleted               boolean     NOT NULL,
    size_bytes            int8        NOT NULL,
    created               timestamptz NOT NULL,
    id                    uuid        NOT NULL,
    exec_id               uuid        REFERENCES t_execution ON DELETE CASCADE,
    checksum_method       varchar(10) NOT NULL,
    checksum_value        varchar(64) NOT NULL,
    name                  text        NOT NULL,
    url                   text        NOT NULL,
    input_correlation_ids text[]      NOT NULL DEFAULT '{}',
    PRIMARY KEY (id)
);

CREATE INDEX ON t_outputfile (checksum_value);
CREATE INDEX ON t_outputfile (url);