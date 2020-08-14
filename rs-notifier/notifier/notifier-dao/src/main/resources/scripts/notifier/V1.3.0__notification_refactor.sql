-- change action type
ALTER TABLE t_notification_action ALTER COLUMN action TYPE jsonb USING to_jsonb(action);
-- Rename notification action column
ALTER TABLE t_notification_action RENAME COLUMN action TO metadata;
ALTER TABLE t_notification_action RENAME COLUMN element TO payload;
