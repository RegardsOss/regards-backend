-- Add override property in reference request
ALTER TABLE t_feature_reference_request
ADD COLUMN override_previous_version boolean;
