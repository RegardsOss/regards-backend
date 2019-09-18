create table t_ui_configuration (id bigint, configuration text, application_id varchar(16));
alter table t_ui_configuration add constraint uk_ui_configuration_application_id unique (application_id);
create sequence seq_ui_configuration start 1 increment 50;
