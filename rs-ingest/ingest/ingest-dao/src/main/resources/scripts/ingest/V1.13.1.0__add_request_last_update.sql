ALTER TABLE t_request
    ADD COLUMN IF NOT EXISTS last_update timestamp DEFAULT CURRENT_TIMESTAMP;