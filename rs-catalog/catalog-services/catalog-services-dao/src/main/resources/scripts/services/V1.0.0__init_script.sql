create table t_link_service_dataset (linkId int8 not null, dataset_id varchar(256) not null, primary key (linkId));
create table ta_link_service_dataset_plugins (dataset_id int8 not null, service_configuration_id int8 not null, primary key (dataset_id, service_configuration_id));
alter table t_link_service_dataset add constraint uk_link_service_dataset_dataset_id unique (dataset_id);
create sequence seq_link_service_dataset start 1 increment 50;
alter table ta_link_service_dataset_plugins add constraint fk_plugin_link_service_dataset foreign key (service_configuration_id) references t_plugin_configuration;
alter table ta_link_service_dataset_plugins add constraint fk_link_service_dataset_plugin foreign key (dataset_id) references t_link_service_dataset;
