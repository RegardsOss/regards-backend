-- Create tenant settings
create table t_dynamic_tenant_setting
(
    id            int8         not null,
    name          varchar(255) not null,
    description   text,
    value         text,
    default_value text,
    class_name     varchar(255),
    primary key (id)
);
alter table t_dynamic_tenant_setting
    add constraint uk_dynamic_tenant_setting_name unique (name);
create sequence seq_dynamic_tenant_setting start 1 increment 50;