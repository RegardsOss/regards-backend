alter table t_ui_module drop column defaultDynamicModule;
alter table t_ui_module add column customIconURL varchar(512);
alter table t_ui_module add column home boolean default false;
alter table t_ui_module add column iconType varchar(255);
alter table t_ui_module add column title text;
