alter table t_ui_plugin add column icon_url varchar(255);
create table t_ui_plugin_application_mode (ui_plugin_id int8 not null, application_mode varchar(255) not null, primary key (ui_plugin_id, application_mode));
create table t_ui_plugin_entity_type (ui_plugin_id int8 not null, entity_type varchar(255) not null, primary key (ui_plugin_id, entity_type));
alter table t_ui_plugin_application_mode add constraint fk_ui_plugin_application_mode_ui_plugin_id foreign key (ui_plugin_id) references t_ui_plugin;
alter table t_ui_plugin_entity_type add constraint fk_ui_plugin_entity_type_ui_plugin_id foreign key (ui_plugin_id) references t_ui_plugin;
