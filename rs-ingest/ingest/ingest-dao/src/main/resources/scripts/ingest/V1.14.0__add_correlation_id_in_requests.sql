ALTER TABLE t_request
    ADD COLUMN IF NOT EXISTS correlation_id varchar(255);