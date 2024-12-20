alter table t_workermanager_request
-- add new column to backup workflow original content
    add column original_content bytea,
-- add new column for headers for propagation between workflow workers
    add column headers jsonb;
