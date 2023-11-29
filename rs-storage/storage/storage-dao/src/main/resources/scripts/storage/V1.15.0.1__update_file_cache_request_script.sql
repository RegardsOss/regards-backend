-- availability_hours column replaces expiration_date column
-- availability_hours column: duration in hours of available files in the cache internal or external (by default 24h)
alter table t_file_cache_request
    add column availability_hours int2 not null default 24;
alter table t_file_cache_request
    drop column if exists expiration_date;