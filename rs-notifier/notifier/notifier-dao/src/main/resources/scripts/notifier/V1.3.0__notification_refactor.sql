-- change action type
ALTER TABLE t_notification_action ALTER COLUMN action TYPE jsonb USING to_jsonb(action);
-- Rename notification column
ALTER TABLE t_notification_action RENAME COLUMN action TO metadata;
ALTER TABLE t_notification_action RENAME COLUMN element TO payload;
ALTER TABLE t_notification_action RENAME COLUMN action_date TO request_date;
-- add requestId
ALTER TABLE t_notification_action ADD COLUMN request_id varchar(36);
-- rename table
ALTER TABLE t_notification_action RENAME TO t_notification_request;
