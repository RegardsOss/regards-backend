ALTER TABLE t_file_reference
    ADD COLUMN pending boolean DEFAULT FALSE;
create index IF NOT EXISTS idx_t_file_ref_pendings on t_file_reference (pending);
ALTER TABLE t_storage_location
    ADD COLUMN nb_ref_pending int8 DEFAULT 0;