-- error_page_nb is removed in favor of error_cursor, which contains more information on how to restart a crawling
ALTER TABLE t_datasource_ingestion
DROP COLUMN error_page_nb;

ALTER TABLE t_datasource_ingestion
ADD COLUMN cursor_position               int8    DEFAULT 0,
ADD COLUMN cursor_previous_last_entity_date  timestamp;