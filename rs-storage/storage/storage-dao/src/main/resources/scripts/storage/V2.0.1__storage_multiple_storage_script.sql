alter table t_data_file drop constraint fk_data_file_data_storage_plugin_configuration;
alter table t_data_file drop column data_storage_plugin_configuration;
alter table t_data_file add column not_yet_stored_by int8;
create table ta_data_file_plugin_conf (data_file_id int8 not null, data_storage_conf_id int8 not null, primary key (data_file_id, data_storage_conf_id));
alter table ta_data_file_plugin_conf add constraint fk_plugin_conf_data_file_plugin_conf foreign key (data_storage_conf_id) references t_plugin_configuration;
alter table ta_data_file_plugin_conf add constraint fk_data_file_plugin_conf_data_file foreign key (data_file_id) references t_data_file;
