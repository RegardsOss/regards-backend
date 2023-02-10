CREATE INDEX IF NOT EXISTS idx_notification_status_id on t_notification USING btree (status, id);
CREATE INDEX IF NOT EXISTS idx_notification_role_name_notification_id ON ta_notification_role_name USING btree (notification_id);
CREATE INDEX IF NOT EXISTS idx_notification_projectuser_email_notification_id on ta_notification_projectuser_email USING btree (notification_id);
