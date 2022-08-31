ALTER TABLE t_ingest_processing_chain
    ADD COLUMN aip_storage_metadata_conf_id int8 DEFAULT NULL;
ALTER TABLE t_ingest_processing_chain
    ADD CONSTRAINT  fk_aip_storage_metadata_conf_id FOREIGN KEY (aip_storage_metadata_conf_id) REFERENCES t_plugin_configuration;
