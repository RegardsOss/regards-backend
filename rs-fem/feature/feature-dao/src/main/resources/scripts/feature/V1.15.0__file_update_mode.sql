-- Add file mode behavior for file update
alter table t_feature_request
    add column IF NOT EXISTS file_update_mode varchar(20);