CREATE TABLE IF NOT EXISTS t_file_storage_request (
                                                id int8 NOT NULL,
                                                creation_date timestamp,
                                                error_cause varchar(512),
                                                job_id varchar(255),
                                                owner varchar(256) NOT NULL,
                                                algorithm varchar(16) NOT NULL,
                                                checksum varchar(128) NOT NULL,
                                                filename varchar(256) NOT NULL,
                                                filesize int8,
                                                height integer,
                                                mime_type varchar(255) NOT NULL,
                                                type varchar(256),
                                                width integer,
                                                origin_url varchar(2048),
                                                status int8 NOT NULL,
                                                status_string varchar(255) NOT NULL,
                                                storage varchar(128),
                                                storage_subdirectory varchar(2048),
                                                session_owner varchar(128),
                                                session_name varchar(128),
                                                PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_file_storage_request ON t_file_storage_request (storage, checksum);
CREATE INDEX IF NOT EXISTS idx_file_storage_request_cs ON t_file_storage_request (checksum);
CREATE INDEX IF NOT EXISTS idx_file_storage_request_storage ON t_file_storage_request (storage);

CREATE TABLE IF NOT EXISTS t_file_reference (
                                          id int8 NOT NULL,
                                          storage varchar(128),
                                          url varchar(2048),
                                          algorithm varchar(16) NOT NULL,
                                          checksum varchar(128) NOT NULL,
                                          filename varchar(256) NOT NULL,
                                          filesize int8,
                                          height integer,
                                          mime_type varchar(255) NOT NULL,
                                          type varchar(256),
                                          width integer,
                                          storagedate timestamp,
                                          referenced boolean DEFAULT false,
                                          pending boolean DEFAULT false,
                                          nearline_confirmed boolean DEFAULT false,
                                          PRIMARY KEY (id)
);

ALTER TABLE t_file_reference DROP CONSTRAINT IF EXISTS uk_t_file_reference_checksum_storage;
ALTER TABLE t_file_reference ADD CONSTRAINT uk_t_file_reference_checksum_storage UNIQUE (checksum, storage);

CREATE INDEX IF NOT EXISTS idx_file_reference_checksum ON t_file_reference (checksum);
CREATE INDEX IF NOT EXISTS idx_file_reference_storage ON t_file_reference (storage);
CREATE INDEX IF NOT EXISTS idx_file_reference_storage_checksum ON t_file_reference (checksum, storage);
CREATE INDEX IF NOT EXISTS idx_file_reference_type ON t_file_reference (type);
CREATE INDEX IF NOT EXISTS idx_file_reference_url ON t_file_reference (url);
CREATE INDEX IF NOT EXISTS idx_nearline_confirmed ON t_file_reference (nearline_confirmed);
CREATE INDEX IF NOT EXISTS idx_t_file_ref_pendings ON t_file_reference (pending);

CREATE TABLE IF NOT EXISTS ta_file_reference_owner (
    file_ref_id int8 NOT NULL,
    owner       varchar(255)
);
ALTER TABLE ta_file_reference_owner
    ADD CONSTRAINT fk_file_ref_owner FOREIGN KEY (file_ref_id) REFERENCES
        t_file_reference ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_ta_file_ref_owner_owner on ta_file_reference_owner (owner);
CREATE INDEX IF NOT EXISTS idx_ta_file_ref_owner_file_id on ta_file_reference_owner (file_ref_id);

CREATE TABLE IF NOT EXISTS t_file_deletion_request (
                                                 id int8 NOT NULL,
                                                 file_reference int8 NOT NULL,
                                                 creation_date timestamp,
                                                 error_cause varchar(512),
                                                 force_delete boolean,
                                                 group_id varchar(128) NOT NULL,
                                                 job_id varchar(255),
                                                 status int8 NOT NULL,
                                                 storage varchar(128) NOT NULL,
                                                 session_owner varchar(128),
                                                 session_name varchar(128),
                                                 PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_file_deletion_file_ref ON t_file_deletion_request (file_reference);
CREATE INDEX IF NOT EXISTS idx_file_deletion_grp ON t_file_deletion_request (group_id, status);
CREATE INDEX IF NOT EXISTS idx_file_deletion_request ON t_file_deletion_request (storage);

CREATE TABLE IF NOT EXISTS ta_storage_request_group_ids (file_storage_request_id int8 NOT NULL, group_id varchar(128)
    NOT NULL, PRIMARY KEY (file_storage_request_id, group_id));

ALTER TABLE t_file_deletion_request DROP CONSTRAINT IF EXISTS fk_t_file_deletion_request_t_file_reference_id;
ALTER TABLE t_file_deletion_request ADD CONSTRAINT fk_t_file_deletion_request_t_file_reference_id FOREIGN KEY (file_reference)
    REFERENCES t_file_reference(id);

ALTER TABLE ta_storage_request_group_ids DROP CONSTRAINT IF EXISTS
    fk_ta_storage_request_group_ids_t_file_storage_request;
ALTER TABLE ta_storage_request_group_ids ADD CONSTRAINT fk_ta_storage_request_group_ids_t_file_storage_request
    FOREIGN KEY (file_storage_request_id) REFERENCES t_file_storage_request ON DELETE CASCADE;

CREATE SEQUENCE IF NOT EXISTS seq_file_reference START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS seq_file_storage_request START 1 INCREMENT 50;