ALTER TABLE t_file_storage_request
    ALTER COLUMN error_cause TYPE VARCHAR(32768);

ALTER TABLE t_file_deletion_request
    ALTER COLUMN error_cause TYPE VARCHAR(32768);

ALTER TABLE t_request_result_info
    ALTER COLUMN error_cause TYPE VARCHAR(32768);
