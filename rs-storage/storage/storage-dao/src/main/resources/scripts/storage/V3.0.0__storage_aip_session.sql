alter table t_aip add column session varchar(128);

CREATE TABLE t_aip_session(
	id VARCHAR(100) NOT NULL,
	last_activation_date TIMESTAMP,
	PRIMARY KEY(id)
);

ALTER TABLE t_aip RENAME COLUMN submissiondate TO submission_date;
ALTER TABLE t_aip ALTER COLUMN state TYPE varchar(32);

ALTER TABLE t_aip_tag RENAME COLUMN tags TO value;
ALTER TABLE t_aip_tag ALTER COLUMN value TYPE varchar(200);

ALTER TABLE t_cached_file ALTER COLUMN checksum TYPE varchar(128);
ALTER TABLE t_cached_file RENAME COLUMN lastrequestdate to last_request_date;
ALTER TABLE t_cached_file RENAME COLUMN filesize to file_size;
ALTER TABLE t_cached_file RENAME COLUMN failurecause to failure_cause;

ALTER TABLE t_data_file RENAME COLUMN storagedirectory to storage_directory;
ALTER TABLE t_data_file RENAME COLUMN mimetype to mime_type;
ALTER TABLE t_data_file RENAME COLUMN datatype to data_type;
ALTER TABLE t_data_file RENAME COLUMN filesize to file_size;

