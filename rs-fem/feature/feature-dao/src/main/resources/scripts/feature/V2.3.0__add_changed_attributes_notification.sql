ALTER TABLE t_feature_request
    ADD COLUMN IF NOT EXISTS changed_attributes_to_notify TEXT
        CHECK (request_type = 'UPDATE' OR changed_attributes_to_notify IS NULL);