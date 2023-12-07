-- true: file is stored on nearline storage
-- false: need to check if file is nearline or online
alter table t_file_reference
    add column if not exists nearline_confirmed boolean default false;
create index if not exists idx_nearline_confirmed
    on t_file_reference (nearline_confirmed);