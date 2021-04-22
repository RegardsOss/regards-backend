CREATE TABLE t_rights_plugin_configuration (
    id int8 NOT NULL,
    plugin_configuration_id int8 NOT NULL,
    process_business_id uuid NOT NULL,
    user_role text,
    datasets varchar(128)[], -- t_entity ipId iv varchar(128)
    PRIMARY KEY (id)
);

CREATE INDEX idx_rights_plugin_configuration_datasets ON t_rights_plugin_configuration USING GIN(datasets);

ALTER TABLE t_rights_plugin_configuration
    ADD CONSTRAINT fk_rights_plugin_configuration
    FOREIGN KEY (plugin_configuration_id)
    REFERENCES t_plugin_configuration;

CREATE SEQUENCE seq_plugin_rights_conf START 1 INCREMENT 50;