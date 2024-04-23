-- Add missing index used by query to search for requests to schedule
CREATE INDEX idx_feature_request_type_step_pid ON t_feature_request USING btree (request_type, step, provider_id)