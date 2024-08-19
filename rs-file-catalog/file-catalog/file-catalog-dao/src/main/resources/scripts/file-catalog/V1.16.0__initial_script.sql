CREATE TABLE IF NOT EXISTS t_file_storage_request (
    id                   INT8 NOT NULL,
    creation_date        TIMESTAMP,
    error_cause          VARCHAR(512),
    job_id               VARCHAR(255),
    owner                VARCHAR(256) NOT NULL,
    algorithm            VARCHAR(16) NOT NULL,
    checksum             VARCHAR(128) NOT NULL,
    filename             VARCHAR(256) NOT NULL,
    filesize             INT8,
    height               INT8,
    mime_type            VARCHAR(255) NOT NULL,
    type                 VARCHAR(256),
    width                INT8,
    origin_url           VARCHAR(2048),
    status               INT8 NOT NULL,
    status_string        VARCHAR(255) NOT NULL,
    storage              VARCHAR(128),
    storage_subdirectory VARCHAR(2048),
    session_owner        VARCHAR(128),
    session_name         VARCHAR(128),
    reference            BOOL,
    PRIMARY KEY (id)
    );

CREATE INDEX IF NOT EXISTS idx_file_storage_request
    ON t_file_storage_request (storage, checksum);

CREATE INDEX IF NOT EXISTS idx_file_storage_request_cs
    ON t_file_storage_request (checksum);

CREATE INDEX IF NOT EXISTS idx_file_storage_request_storage
    ON t_file_storage_request (storage);

CREATE TABLE IF NOT EXISTS t_file_reference (
    id                 INT8 NOT NULL,
    storage            VARCHAR(128),
    url                VARCHAR(2048),
    algorithm          VARCHAR(16) NOT NULL,
    checksum           VARCHAR(128) NOT NULL,
    filename           VARCHAR(256) NOT NULL,
    filesize           INT8,
    height             INT8,
    mime_type          VARCHAR(255) NOT NULL,
    type               VARCHAR(256),
    width              INT8,
    storagedate        TIMESTAMP,
    referenced         BOOL DEFAULT false,
    pending            BOOL DEFAULT false,
    nearline_confirmed BOOL DEFAULT false,
    PRIMARY KEY (id)
    );

ALTER TABLE t_file_reference
DROP CONSTRAINT IF EXISTS uk_t_file_reference_checksum_storage;

ALTER TABLE t_file_reference
    ADD CONSTRAINT uk_t_file_reference_checksum_storage UNIQUE (checksum, storage);

CREATE INDEX IF NOT EXISTS idx_file_reference_checksum
    ON t_file_reference (checksum);

CREATE INDEX IF NOT EXISTS idx_file_reference_storage
    ON t_file_reference (storage);

CREATE INDEX IF NOT EXISTS idx_file_reference_storage_checksum
    ON t_file_reference (checksum, storage);

CREATE INDEX IF NOT EXISTS idx_file_reference_type
    ON t_file_reference (type);

CREATE INDEX IF NOT EXISTS idx_file_reference_url
    ON t_file_reference (url);

CREATE INDEX IF NOT EXISTS idx_nearline_confirmed
    ON t_file_reference (nearline_confirmed);

CREATE INDEX IF NOT EXISTS idx_t_file_ref_pendings
    ON t_file_reference (pending);

CREATE TABLE IF NOT EXISTS ta_file_reference_owner (
    file_ref_id INT8 NOT NULL, owner VARCHAR(255)
    );

ALTER TABLE ta_file_reference_owner
    ADD CONSTRAINT fk_file_ref_owner FOREIGN KEY (file_ref_id) REFERENCES t_file_reference ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_ta_file_ref_owner_owner
    ON ta_file_reference_owner (owner);

CREATE INDEX IF NOT EXISTS idx_ta_file_ref_owner_file_id
    ON ta_file_reference_owner (file_ref_id);

CREATE TABLE IF NOT EXISTS t_file_deletion_request (
    id             INT8 NOT NULL,
    file_reference INT8 NOT NULL,
    creation_date  TIMESTAMP,
    error_cause    VARCHAR(512),
    force_delete   BOOL,
    group_id       VARCHAR(128) NOT NULL,
    job_id         VARCHAR(255),
    status         INT8 NOT NULL,
    storage        VARCHAR(128) NOT NULL,
    session_owner  VARCHAR(128),
    session_name   VARCHAR(128),
    PRIMARY KEY (id)
    );

CREATE INDEX IF NOT EXISTS idx_file_deletion_file_ref
    ON t_file_deletion_request (file_reference);

CREATE INDEX IF NOT EXISTS idx_file_deletion_grp
    ON t_file_deletion_request (group_id, status);

CREATE INDEX IF NOT EXISTS idx_file_deletion_request
    ON t_file_deletion_request (storage);

CREATE TABLE IF NOT EXISTS ta_storage_request_group_ids (
    file_storage_request_id INT8 NOT NULL,
    group_id                VARCHAR(128) NOT NULL,
    PRIMARY KEY (file_storage_request_id, group_id)
    );

ALTER TABLE t_file_deletion_request
DROP CONSTRAINT IF EXISTS fk_t_file_deletion_request_t_file_reference_id;

ALTER TABLE t_file_deletion_request
    ADD CONSTRAINT fk_t_file_deletion_request_t_file_reference_id FOREIGN KEY (file_reference) REFERENCES t_file_reference (id);

ALTER TABLE ta_storage_request_group_ids
DROP CONSTRAINT IF EXISTS fk_ta_storage_request_group_ids_t_file_storage_request;

ALTER TABLE ta_storage_request_group_ids
    ADD CONSTRAINT fk_ta_storage_request_group_ids_t_file_storage_request FOREIGN KEY (file_storage_request_id) REFERENCES t_file_storage_request ON DELETE CASCADE;

CREATE SEQUENCE IF NOT EXISTS seq_file_reference START 1 INCREMENT 50;

CREATE SEQUENCE IF NOT EXISTS seq_file_storage_request START 1 INCREMENT 50;

CREATE TABLE IF NOT EXISTS t_request_result_info (
    id                 INT8 NOT NULL,
    error              BOOL,
    error_cause        VARCHAR(512),
    group_id           VARCHAR(128) NOT NULL,
    request_checksum   VARCHAR(128) NOT NULL,
    request_owners     JSONB,
    request_storage    VARCHAR(128),
    request_store_path VARCHAR(2048),
    request_type       VARCHAR(255) NOT NULL,
    result_file_ref_id INT8,
    PRIMARY KEY (id)
    );

ALTER TABLE t_request_result_info
    ADD CONSTRAINT fkc9bxmi7kd5qm75tg4tmxfflc8 FOREIGN KEY (result_file_ref_id) REFERENCES t_file_reference;

CREATE SEQUENCE seq_groups_requests_info START WITH 1 INCREMENT 50;

CREATE INDEX idx_group_id ON t_request_result_info (
                                                    group_id
    );

CREATE INDEX idx_group_file_ref_id
    ON t_request_result_info (result_file_ref_id);

CREATE TABLE t_request_group (
                                 id              VARCHAR(255) NOT NULL,
                                 creation_date   TIMESTAMP NOT NULL,
                                 expiration_date TIMESTAMP NOT NULL,
                                 type            VARCHAR(255) NOT NULL,
                                 PRIMARY KEY (id)
);

CREATE INDEX idx_t_request_group ON t_request_group (id);

CREATE SEQUENCE seq_request_group START WITH 1 INCREMENT 50;