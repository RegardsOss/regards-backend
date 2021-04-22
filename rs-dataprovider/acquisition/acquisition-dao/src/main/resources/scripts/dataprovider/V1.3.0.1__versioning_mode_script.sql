ALTER TABLE t_acq_processing_chain ADD COLUMN versioning_mode varchar(20);
UPDATE t_acq_processing_chain SET versioning_mode = 'INC_VERSION';
ALTER TABLE t_acq_processing_chain ALTER COLUMN versioning_mode SET  NOT NULL;