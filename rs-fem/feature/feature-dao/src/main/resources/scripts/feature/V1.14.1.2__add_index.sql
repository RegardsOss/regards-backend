-- the index idx_feature_request_id is working on request_id so its name is invalid
-- let's recreate it
DROP INDEX IF EXISTS idx_feature_request_id;
CREATE INDEX IF NOT EXISTS idx_feature_request_request_id ON t_feature_request USING btree (request_id);

CREATE INDEX IF NOT EXISTS idx_feature_request_id ON t_feature_request USING btree (id);

CREATE INDEX IF NOT EXISTS idx_feature_id ON t_feature USING btree (id);
