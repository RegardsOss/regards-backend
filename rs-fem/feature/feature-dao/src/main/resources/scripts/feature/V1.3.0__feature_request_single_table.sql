create table t_feature_request (id int8 not null, request_type varchar(20) not null, provider_id varchar(100), session_owner varchar(128),
session_name varchar(128), storages jsonb, errors jsonb, request_id varchar(36) not null, request_owner varchar(128) not null,
state varchar(50) not null, feature jsonb, registration_date timestamp not null, request_date timestamp not null,
step varchar(50) not null, priority numeric not null, group_id varchar(255), feature_id int8, override_previous_version boolean,
urn varchar(132), storage varchar(132), checksum varchar(132), primary key (id));

create index idx_feature_request_id on t_feature_request (request_id);
create index idx_feature_request_type on t_feature_request (request_type);
create index idx_feature_request_state on t_feature_request (state);
create index idx_feature_request_urn on t_feature_request (urn);
create index idx_feature_request_step_registration_priority on t_feature_request (step, registration_date, priority);
create index idx_feature_request_group_id on t_feature_request (group_id);

alter table t_feature_request add constraint uk_feature_request_id unique (request_id);
create sequence seq_feature_request start 1 increment 50;

alter table t_feature_request add constraint fk_feature_id foreign key (feature_id) references t_feature;