CREATE
	TABLE
		t_email(
			id int8 NOT NULL,
			att_name VARCHAR(100),
			attachment bytea,
			bcc_addrs VARCHAR(1000),
			cc_addrs VARCHAR(1000),
			from_addr VARCHAR(320),
			reply_to_addr VARCHAR(320),
			sent_date TIMESTAMP,
			subject VARCHAR(78),
			text text,
			to_addrs VARCHAR(1000),
			PRIMARY KEY(id)
		);

CREATE
	SEQUENCE seq_email START 1 INCREMENT 50;

