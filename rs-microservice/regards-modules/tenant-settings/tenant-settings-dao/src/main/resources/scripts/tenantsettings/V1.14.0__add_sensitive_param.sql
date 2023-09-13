-- add sensitive parameter
ALTER TABLE t_dynamic_tenant_setting
    ADD COLUMN contains_sensitive_params boolean DEFAULT FALSE NOT NULL;