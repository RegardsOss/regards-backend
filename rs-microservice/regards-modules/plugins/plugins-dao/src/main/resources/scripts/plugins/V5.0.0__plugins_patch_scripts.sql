-- Clean database
ALTER TABLE t_plugin_param_dyn_value DROP CONSTRAINT fk_plugin_param_dyn_value_param_id;
DROP TABLE t_plugin_param_dyn_value;
ALTER TABLE t_plugin_parameter DROP CONSTRAINT fk_param_next_conf_id;
ALTER TABLE t_plugin_parameter DROP CONSTRAINT fk_plg_conf_param_id;
DROP SEQUENCE seq_plugin_parameter;
DROP TABLE t_plugin_parameter;

-- Add business key
ALTER TABLE t_plugin_configuration ADD COLUMN bid varchar(36) NOT null;
CREATE INDEX idx_plugin_configuration_bid on t_plugin_configuration (bid);
-- Switch unique constraint to business key
ALTER TABLE t_plugin_configuration DROP CONSTRAINT uk_plugin_configuration_label;
ALTER TABLE t_plugin_configuration ADD CONSTRAINT uk_plugin_bid UNIQUE (bid);

-- Switch parameters to jsonb type
ALTER TABLE t_plugin_configuration ADD COLUMN parameters jsonb;



