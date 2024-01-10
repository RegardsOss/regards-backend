-- indexes idx_worker_request_id and idx_worker_request_id
-- are not correctly named, let's rename them
DROP INDEX IF EXISTS idx_worker_request_id;
DROP INDEX IF EXISTS idx_worker_request_content_type;
CREATE INDEX IF NOT EXISTS idx_workermanager_request_request_id ON t_workermanager_request USING btree (request_id);
CREATE INDEX IF NOT EXISTS idx_workermanager_request_content_type ON t_workermanager_request USING btree (content_type);

-- add index on id
CREATE INDEX IF NOT EXISTS idx_workermanager_request_id ON t_workermanager_request USING btree (id);
