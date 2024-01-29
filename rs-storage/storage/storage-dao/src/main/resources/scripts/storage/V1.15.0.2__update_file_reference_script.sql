-- true: file is stored on nearline storage
-- false: need to check if file is nearline or online
alter table t_file_reference
    add column IF NOT EXISTS nearline_confirmed boolean default false;
-- index on nearline_confirmed column
create index IF NOT EXISTS idx_file_reference_nearline_confirmed on t_file_reference (nearline_confirmed);
-- index on url column
create index IF NOT EXISTS idx_file_reference_url on t_file_reference (url);