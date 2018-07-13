/* Search */
create table t_search_engine_conf (id int8 not null, dataset_urn varchar(256), plugin_conf_id int8, primary key (id));
create sequence seq_search_engine_conf start 1 increment 50;
alter table t_search_engine_conf add constraint FKcwuph24678s29jii6koj4se8v foreign key (plugin_conf_id) references t_plugin_configuration;
