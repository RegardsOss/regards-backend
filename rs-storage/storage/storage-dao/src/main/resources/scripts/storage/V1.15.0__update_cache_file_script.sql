-- true: internal cache / false: external cache
alter table t_cache_file
    add column IF NOT EXISTS internal_cache boolean not null default true;
-- business identifier of plugin to access file in external cache
alter table t_cache_file
    add column IF NOT EXISTS external_cache_plugin varchar(36) default null;