/* Plugins */
create table t_plugin_configuration (id int8 not null, active boolean, interfaceNames text, label varchar(255), pluginClassName varchar(255), pluginId varchar(255) not null, priorityOrder int4 not null, version varchar(255) not null, primary key (id));
create table t_plugin_param_dyn_value (id int8 not null, value varchar(255));
create table t_plugin_parameter (id int8 not null, dynamic boolean, name varchar(255) not null, value text, next_conf_id int8, parent_conf_id int8, primary key (id));
create index idx_plugin_configuration on t_plugin_configuration (pluginId);
create index idx_plugin_configuration_label on t_plugin_configuration (label);
alter table t_plugin_configuration add constraint uk_plugin_configuration_label unique (label);
create sequence seq_plugin_conf start 1 increment 50;
create sequence seq_plugin_parameter start 1 increment 50;
alter table t_plugin_param_dyn_value add constraint fk_plugin_param_dyn_value_param_id foreign key (id) references t_plugin_parameter;
alter table t_plugin_parameter add constraint fk_param_next_conf_id foreign key (next_conf_id) references t_plugin_configuration;
alter table t_plugin_parameter add constraint fk_plg_conf_param_id foreign key (parent_conf_id) references t_plugin_configuration;
