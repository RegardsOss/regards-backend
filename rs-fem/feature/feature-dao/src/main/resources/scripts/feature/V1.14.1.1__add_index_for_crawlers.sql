-- Creates specific index for feature datasource crawler. This crawler requests feature with two parameters model and
-- last_update.
CREATE INDEX IF NOT EXISTS idx_feature_model_last_update on t_feature (model, last_update);

-- Remove duplicated index
DROP INDEX IF EXISTS idx_feature_entity_provider_id;
DROP INDEX IF EXISTS idx_feature_urn;