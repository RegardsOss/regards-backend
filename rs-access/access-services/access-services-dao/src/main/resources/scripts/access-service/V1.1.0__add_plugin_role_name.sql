ALTER TABLE t_ui_plugin ADD role_name varchar(255);
UPDATE t_ui_plugin SET role_name = 'PUBLIC';
ALTER TABLE t_ui_plugin ALTER COLUMN role_name SET NOT NULL;
