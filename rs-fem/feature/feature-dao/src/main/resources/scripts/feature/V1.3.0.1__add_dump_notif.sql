-- Add dump functioning to store metadata

-- Create feature save metadata request
create table t_feature_metadata_request (id int8 not null, errors jsonb, registration_date timestamp not null,
request_date timestamp not null, request_id varchar(36) not null, request_owner varchar(128) not null,
state varchar(50) not null, group_id varchar(255), step varchar(50) not null, urn varchar(132) not null,
priority numeric not null, dump_location varchar(260), previous_dump_date timestamp, primary key (id));
alter table t_feature_metadata_request add constraint uk_feature_metadata_request_id unique (request_id);
create index idx_feature_metadata_request_id on t_feature_metadata_request (request_id);
create index idx_feature_metadata_request_state on t_feature_metadata_request (state);
create index idx_feature_metadata_request_urn on t_feature_metadata_request (urn);
create index idx_feature_metadata_step_registration_priority on t_feature_metadata_request (step, registration_date, priority);
create sequence seq_metadata_request start 1 increment 50;

alter table t_feature_request add dump_location varchar(260);
alter table t_feature_request add previous_dump_date timestamp;

-- Add notification settings to handle optional notifications on features
create table t_feature_notification_settings (id int8 not null, active_notifications boolean not null default true);
alter table t_feature_notification_settings add constraint uk_t_feature_notification_settings unique (id);