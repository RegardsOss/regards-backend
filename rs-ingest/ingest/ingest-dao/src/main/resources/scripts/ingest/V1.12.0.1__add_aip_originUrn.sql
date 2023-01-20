ALTER TABLE t_aip
    ADD COLUMN origin_urn varchar(128);
CREATE INDEX idx_origin_urn ON t_aip (origin_urn varchar_pattern_ops);