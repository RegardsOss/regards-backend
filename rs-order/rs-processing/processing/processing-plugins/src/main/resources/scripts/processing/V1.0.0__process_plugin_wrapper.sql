create table t_rights_plugin_configuration (
    id int8 not null,
    plugin_configuration_id int8 not null,
    tenant text,
    user_role text,
    datasets int8[],
    primary key (id)
);

alter table t_rights_plugin_configuration
    add constraint fk_rights_plugin_configuration
    foreign key (plugin_configuration_id)
    references t_plugin_configuration;

create sequence seq_plugin_rights_conf start 1 increment 50;