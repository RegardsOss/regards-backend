-- Add extraction parameters and drop location column
ALTER TABLE t_feature_reference_request ADD COLUMN extraction_parameters jsonb not null;
ALTER TABLE t_feature_reference_request DROP COLUMN location;
-- Rename factory column
ALTER TABLE t_feature_reference_request RENAME COLUMN plugin_business_id TO extraction_factory;
