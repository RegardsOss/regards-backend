-- Add missing on delete cascade on foreign key constraint between features and dissemination info
ALTER TABLE t_feature_dissemination_info
DROP CONSTRAINT fk_feature_dissemination_info_feature_id,
    ADD CONSTRAINT fk_feature_dissemination_info_feature_id
        FOREIGN KEY (feature_id)
            REFERENCES t_feature (id)
            ON DELETE CASCADE;