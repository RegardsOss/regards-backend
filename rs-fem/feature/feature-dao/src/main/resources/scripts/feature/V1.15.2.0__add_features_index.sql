CREATE INDEX IF NOT EXISTS idx_feature_request_feature_id ON t_feature_request USING btree (feature_id);
CREATE INDEX IF NOT EXISTS idx_feature_dissemination_info_feature_id ON t_feature_dissemination_info USING btree (feature_id);
