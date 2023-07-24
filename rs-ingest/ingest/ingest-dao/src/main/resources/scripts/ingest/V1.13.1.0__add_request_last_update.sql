ALTER TABLE t_request
    ADD COLUMN IF NOT EXISTS last_update timestamp DEFAULT CURRENT_TIMESTAMP;
create index IF NOT EXISTS idx_request_state_last_update on t_request (state, last_update);