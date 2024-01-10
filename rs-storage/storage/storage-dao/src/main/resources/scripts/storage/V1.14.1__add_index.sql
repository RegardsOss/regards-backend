CREATE INDEX IF NOT EXISTS idx_file_storage_request_id ON t_file_storage_request USING btree (id);
CREATE INDEX IF NOT EXISTS idx_file_reference_storage_checksum on t_file_reference (checksum, storage);
