ALTER TABLE t_file_cache_request ADD job_id varchar(36);
ALTER TABLE t_file_storage_request ADD job_id varchar(36);
ALTER TABLE t_file_deletion_request ADD job_id varchar(36);
