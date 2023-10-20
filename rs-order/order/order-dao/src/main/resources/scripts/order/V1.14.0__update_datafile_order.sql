ALTER TABLE t_data_file
    ADD COLUMN version int4 NOT NULL DEFAULT 0;

-- make correlationId mandatory
UPDATE t_order
SET correlation_id='auto-correlation-id-' || id
where correlation_id IS NULL;

ALTER TABLE t_order
    ALTER COLUMN correlation_id SET NOT NULL;
