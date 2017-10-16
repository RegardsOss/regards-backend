CREATE
	TABLE
		t_notification(
			id int8 NOT NULL,
			DATE TIMESTAMP,
			message VARCHAR(255),
			sender VARCHAR(255),
			status int4,
			title VARCHAR(255),
			PRIMARY KEY(id)
		);

CREATE
	TABLE
		t_notification_settings(
			id int8 NOT NULL,
			days int4,
			frequency int4,
			hours int4,
			user_id int8,
			PRIMARY KEY(id)
		);

CREATE
	TABLE
		ta_notification_projectuser(
			notification_id int8 NOT NULL,
			projectuser_id int8 NOT NULL
		);

CREATE
	TABLE
		ta_notification_role(
			notification_id int8 NOT NULL,
			role_id int8 NOT NULL
		);

CREATE
	SEQUENCE seq_notification START 1 INCREMENT 50;

CREATE
	SEQUENCE seq_notification_settings START 1 INCREMENT 50;

ALTER TABLE
	t_notification_settings ADD CONSTRAINT fk_notification_settings_user FOREIGN KEY(user_id) REFERENCES t_project_user;

ALTER TABLE
	ta_notification_projectuser ADD CONSTRAINT fk_projectuser_notification FOREIGN KEY(projectuser_id) REFERENCES t_project_user;

ALTER TABLE
	ta_notification_projectuser ADD CONSTRAINT fk_notification_projectuser FOREIGN KEY(notification_id) REFERENCES t_notification;

ALTER TABLE
	ta_notification_role ADD CONSTRAINT fk_role_notification FOREIGN KEY(role_id) REFERENCES t_role;

ALTER TABLE
	ta_notification_role ADD CONSTRAINT fk_notification_role FOREIGN KEY(notification_id) REFERENCES t_notification;
