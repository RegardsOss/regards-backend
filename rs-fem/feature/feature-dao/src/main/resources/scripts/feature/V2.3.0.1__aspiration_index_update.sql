-- Update an index for GeoJson products to improve performance of aspirations
DROP INDEX IF EXISTS idx_feature_model_last_update;
CREATE INDEX IF NOT EXISTS idx_feature_model_last_update_id ON t_feature USING btree (model, last_update, id);
