alter table t_access_settings add column default_role_id int8;
alter table t_access_settings add constraint fk_access_settings_default_role foreign key (default_role_id) references t_role;
alter table t_access_settings add column default_groups text[] not null default '{}';
