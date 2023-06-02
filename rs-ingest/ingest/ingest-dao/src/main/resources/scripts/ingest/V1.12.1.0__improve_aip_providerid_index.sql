DROP INDEX IF EXISTS idx_aip_provider_id;
CREATE INDEX idx_aip_provider_id ON t_aip (provider_id varchar_pattern_ops);
-- INDEX alternatif permettant les recherches like %value% :
-- CREATE INDEX idx_aip_provider_id ON t_aip using GIN (provider_id gin_trgm_ops)
-- Attention ce type d'index nécessite l'installation de l'extension pg_trgm