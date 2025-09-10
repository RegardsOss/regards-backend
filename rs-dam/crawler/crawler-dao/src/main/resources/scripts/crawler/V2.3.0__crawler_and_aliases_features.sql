CREATE SEQUENCE IF NOT EXISTS seq_es_index_alias START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS t_es_index_alias
(
    id             BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_es_index_alias'),
    alias          VARCHAR(255) NOT NULL,
    current_index  VARCHAR(255) NOT NULL,
    building_index VARCHAR(255),
    CONSTRAINT unique_index_alias_alias UNIQUE (alias)
);

ALTER TABLE t_datasource_ingestion
    ADD COLUMN if NOT EXISTS job_id uuid;