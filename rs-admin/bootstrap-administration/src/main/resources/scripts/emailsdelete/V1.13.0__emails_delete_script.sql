-- delete old table t_email on every tenants as the email module has moved to rs-instance
DROP TABLE IF EXISTS t_email;
DROP TABLE IF EXISTS emails_schema_version;
DROP SEQUENCE IF EXISTS seq_email;
