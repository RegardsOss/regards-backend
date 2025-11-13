ALTER TABLE t_datasource_ingestion
    ADD COLUMN if NOT EXISTS job_id uuid;

ALTER TABLE t_datasource_ingestion
    ADD COLUMN IF NOT EXISTS building boolean NOT NULL DEFAULT false;

ALTER TABLE t_datasource_ingestion
    ALTER COLUMN ds_id TYPE varchar(50);

ALTER TABLE t_datasource_ingestion
    ADD COLUMN IF NOT EXISTS last_successful_ingest_date timestamp;