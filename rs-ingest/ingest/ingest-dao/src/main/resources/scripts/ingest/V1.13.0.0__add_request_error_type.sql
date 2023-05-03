-- add error_type column
ALTER table t_request add error_type varchar(100);
CREATE INDEX IF NOT EXISTS idx_error_type ON t_request (error_type varchar_pattern_ops);