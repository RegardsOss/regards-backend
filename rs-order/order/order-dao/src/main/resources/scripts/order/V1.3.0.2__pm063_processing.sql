-- Functionality: PM63, allowing processing on order datasets selections

ALTER TABLE t_basket_dataset ADD COLUMN process_dataset_desc jsonb;

ALTER TABLE t_dataset_task   DROP COLUMN processing_service;
ALTER TABLE t_dataset_task   ADD COLUMN process_batch_desc jsonb;

ALTER TABLE t_data_file      ADD COLUMN process_exec_desc jsonb;
