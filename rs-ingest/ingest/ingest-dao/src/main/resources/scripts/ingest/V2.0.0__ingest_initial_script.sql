CREATE
	TABLE
		t_ingest_processing_chain(
			id int8 NOT NULL,
			description VARCHAR(128),
			name VARCHAR(50) NOT NULL,
			generation_conf_id int8 NOT NULL,
			postprocessing_conf_id int8,
			preprocessing_conf_id int8,
			tag_conf_id int8,
			validation_conf_id int8 NOT NULL,
			PRIMARY KEY(id)
		);

CREATE
	TABLE
		t_sip(
			id int8 NOT NULL,
			checksum VARCHAR(128),
			ingestDate TIMESTAMP,
			ipId VARCHAR(128),
			processing VARCHAR(100),
			sessionId VARCHAR(100),
			rawsip jsonb,
			sipId VARCHAR(100),
			state VARCHAR(255),
			version int4,
			PRIMARY KEY(id)
		);

ALTER TABLE
	t_ingest_processing_chain ADD CONSTRAINT uk_ingest_chain_name UNIQUE(name);

CREATE
	INDEX idx_sip_id ON
	t_sip(
		sipId,
		ipId,
		checksum
	);

ALTER TABLE
	t_sip ADD CONSTRAINT uk_sip_ipId UNIQUE(ipId);

ALTER TABLE
	t_sip ADD CONSTRAINT uk_sip_checksum UNIQUE(checksum);

CREATE
	SEQUENCE seq_ingest_chain START 1 INCREMENT 50;

CREATE
	SEQUENCE seq_sip START 1 INCREMENT 50;

ALTER TABLE
	t_ingest_processing_chain ADD CONSTRAINT fk_generation_conf_id FOREIGN KEY(generation_conf_id) REFERENCES t_plugin_configuration;

ALTER TABLE
	t_ingest_processing_chain ADD CONSTRAINT fk_postprocessing_conf_id FOREIGN KEY(postprocessing_conf_id) REFERENCES t_plugin_configuration;

ALTER TABLE
	t_ingest_processing_chain ADD CONSTRAINT fk_preprocessing_conf_id FOREIGN KEY(preprocessing_conf_id) REFERENCES t_plugin_configuration;

ALTER TABLE
	t_ingest_processing_chain ADD CONSTRAINT fk_tag_conf_id FOREIGN KEY(tag_conf_id) REFERENCES t_plugin_configuration;

ALTER TABLE
	t_ingest_processing_chain ADD CONSTRAINT fk_validation_conf_id FOREIGN KEY(validation_conf_id) REFERENCES t_plugin_configuration;
