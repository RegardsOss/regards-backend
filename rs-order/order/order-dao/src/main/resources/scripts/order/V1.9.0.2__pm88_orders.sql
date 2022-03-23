ALTER TABLE t_dataset_task DROP COLUMN IF EXISTS process_batch_desc;
ALTER TABLE t_dataset_task ADD COLUMN process_dataset_desc jsonb default null;