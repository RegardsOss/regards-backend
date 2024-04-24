-- First drop all previous indexes on table t_feature_request
DROP INDEX IF EXISTS idx_feature_request_group_id;
DROP INDEX IF EXISTS idx_feature_request_id;
DROP INDEX IF EXISTS idx_feature_request_request_id;
DROP INDEX IF EXISTS idx_feature_request_state;
DROP INDEX IF EXISTS idx_feature_request_step_registration_priority;
DROP INDEX IF EXISTS idx_feature_request_urn;
DROP INDEX IF EXISTS idx_feature_step_registration_priority;

-- Add index used by queries in IAbstractFeatureRequestRepository
CREATE INDEX IF NOT EXISTS idx_feature_request_request_id ON t_feature_request USING btree (request_id);
CREATE INDEX IF NOT EXISTS idx_feature_request_urn ON t_feature_request USING btree (urn);
CREATE INDEX IF NOT EXISTS idx_feature_request_step_reqdate ON t_feature_request USING btree (step, request_date);
CREATE INDEX IF NOT EXISTS idx_feature_request_urn_step ON t_feature_request USING btree (urn, step);

-- Add index used by queries in IFeatureCreationRequestRepository
CREATE INDEX IF NOT EXISTS idx_feature_request_type_step_pid ON t_feature_request USING btree (request_type, step,
    provider_id);

-- Add index used by queries in IFeatureDeletionRequestRepository
CREATE INDEX IF NOT EXISTS idx_feature_request_type_urn ON t_feature_request USING btree (request_type, urn);
CREATE INDEX IF NOT EXISTS idx_feature_request_type_step_reqdate ON t_feature_request USING btree (request_type, step,
    request_date);

-- Add index used by queries in all repositories
CREATE INDEX IF NOT EXISTS idx_feature_request_type_groupid ON t_feature_request USING btree (request_type, group_id);
CREATE INDEX IF NOT EXISTS idx_feature_request_type_step ON t_feature_request USING btree (request_type, step);
CREATE INDEX IF NOT EXISTS idx_feature_request_type_step_regdate_reqdate ON t_feature_request USING btree
    (request_type, step, registration_date, request_date);


-- Add index used by specifications
CREATE INDEX IF NOT EXISTS idx_feature_request_type_pid ON t_feature_request USING btree (request_type, provider_id);
CREATE INDEX IF NOT EXISTS idx_feature_request_type_state ON t_feature_request USING btree (request_type, state);
CREATE INDEX IF NOT EXISTS idx_feature_request_type_regdate ON t_feature_request USING btree (request_type,
    registration_date);
CREATE INDEX IF NOT EXISTS idx_feature_request_type_session ON t_feature_request USING btree (request_type,
    session_owner, session_name);
