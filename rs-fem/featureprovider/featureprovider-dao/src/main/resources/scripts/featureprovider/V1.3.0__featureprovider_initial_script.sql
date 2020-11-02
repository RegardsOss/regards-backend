-- extraction request
create table t_feature_extraction_request (id int8 not null, session_owner varchar(128) not null,
session_name varchar(128) not null, storages jsonb, errors jsonb, extraction_parameters jsonb not null, request_id varchar(36) not null, request_owner varchar(128) not null,
state varchar(50) not null, registration_date timestamp not null, request_date timestamp not null, step varchar(50) not null, 
priority numeric not null, extraction_factory varchar(128) not null, override_previous_version boolean, primary key (id));
alter table t_feature_extraction_request add constraint uk_feature_extraction_request_id unique (request_id);
create sequence seq_feature_extraction_request start 1 increment 50;
create index idx_extraction_request_step on t_feature_extraction_request (step);