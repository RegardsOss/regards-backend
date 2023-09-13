ALTER TABLE t_datasource_ingestion
    ADD COLUMN if NOT EXISTS column_id_name varchar(255);
ALTER TABLE t_datasource_ingestion
    ADD COLUMN if NOT EXISTS cursor_last_id int8;
ALTER TABLE t_datasource_ingestion
    ADD COLUMN if NOT EXISTS previous_cursor_last_id int8;