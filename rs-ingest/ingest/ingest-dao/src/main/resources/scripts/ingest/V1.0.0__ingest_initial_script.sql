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
	SEQUENCE seq_sip START 1 INCREMENT 50;
