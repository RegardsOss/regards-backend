ALTER TABLE t_notification_settings
    ADD CONSTRAINT uk_user_email UNIQUE (user_email);