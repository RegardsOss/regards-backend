drop index idx_file_deletion_grp;
create index idx_file_deletion_grp on t_file_deletion_request (group_id, status);

drop index idx_file_cache_request_grp;
create index idx_file_cache_request_grp on t_file_cache_request (group_id, status);

drop index idx_file_copy_request_grp;
create index idx_file_copy_request_grp on t_file_copy_request (group_id, status);


