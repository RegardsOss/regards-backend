CREATE
	TABLE
		t_job_info(
			id uuid NOT NULL,
			class_name VARCHAR(255),
			description text,
			expire_date TIMESTAMP,
			owner VARCHAR(255),
			priority int4,
			RESULT text,
			result_class_name VARCHAR(255),
			estimate_completion TIMESTAMP,
			percent_complete int4,
			stacktrace text,
			start_date TIMESTAMP,
			status VARCHAR(16),
			status_date TIMESTAMP,
			stop_date TIMESTAMP,
			PRIMARY KEY(id)
		);

CREATE
	TABLE
		t_job_parameters(
			job_id uuid NOT NULL,
			class_name VARCHAR(255),
			name VARCHAR(100) NOT NULL,
			value text,
			PRIMARY KEY(
				job_id,
				name
			)
		);

ALTER TABLE
	t_job_parameters ADD CONSTRAINT fk_job_param FOREIGN KEY(job_id) REFERENCES t_job_info;
