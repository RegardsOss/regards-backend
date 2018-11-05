CREATE INDEX idx_aip_tags ON t_aip USING gin  ((json_aip -> 'properties' -> 'pdi' -> 'contextInformation' -> 'tags'));
DROP TABLE t_aip_tag;
