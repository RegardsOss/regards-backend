ALTER TABLE t_datasource_ingestion
    RENAME COLUMN cursor_previous_last_entity_date TO cursor_last_entity_date;

ALTER TABLE t_datasource_ingestion
    ADD COLUMN cursor_previous_last_entity_date timestamp;
