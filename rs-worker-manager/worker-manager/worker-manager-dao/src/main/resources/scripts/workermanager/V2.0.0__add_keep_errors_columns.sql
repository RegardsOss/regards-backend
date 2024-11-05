-- add new columns to avoid keeping worker responses in database if not needed
ALTER TABLE t_worker_conf
    ADD COLUMN keep_errors boolean NOT NULL DEFAULT TRUE;
alter table t_workflow_config
    ADD COLUMN keep_errors boolean NOT NULL DEFAULT TRUE;
