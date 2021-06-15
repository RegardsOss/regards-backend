-- SESSION
-- modify requests for session monitoring
alter table t_file_copy_request add column session_owner varchar(128), add column session_name varchar(128);
alter table t_file_deletion_request add column session_owner varchar(128), add column session_name varchar(128);
alter table t_file_storage_request add column session_owner varchar(128), add column session_name varchar(128);