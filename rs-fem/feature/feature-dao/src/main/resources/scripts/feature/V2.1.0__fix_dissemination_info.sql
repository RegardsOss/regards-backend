ALTER TABLE t_feature_dissemination_info DROP CONSTRAINT IF EXISTS unique_label_and_feature;
ALTER TABLE t_feature_dissemination_info
    ADD CONSTRAINT unique_label_and_feature UNIQUE (label, feature_id);