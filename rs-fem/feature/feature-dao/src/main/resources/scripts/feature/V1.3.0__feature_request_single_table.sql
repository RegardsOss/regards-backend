create table t_feature_request (id int8 not null, request_type varchar(20) not null, provider_id varchar(100), session_owner varchar(128),
session_name varchar(128), storages jsonb, errors jsonb, request_id varchar(36) not null, request_owner varchar(128) not null,
state varchar(50) not null, feature jsonb, registration_date timestamp not null, request_date timestamp not null,
step varchar(50) not null, priority numeric not null, group_id varchar(255), feature_id int8, override_previous_version boolean,
urn varchar(132), storage varchar(132), checksum varchar(132), already_deleted boolean DEFAULT false, to_notify jsonb, primary key (id));

create index idx_feature_request_id on t_feature_request (request_id);
create index idx_feature_request_type on t_feature_request (request_type);
create index idx_feature_request_state on t_feature_request (state);
create index idx_feature_request_urn on t_feature_request (urn);
create index idx_feature_request_step_registration_priority on t_feature_request (step, registration_date, priority);
create index idx_feature_request_group_id on t_feature_request (group_id);

alter table t_feature_request add constraint uk_feature_request_id unique (request_id);
create sequence seq_feature_request start 1 increment 50;
select setval('seq_feature_request', (SELECT max(last_value) FROM ((select last_value from seq_feature_creation_request) UNION (select last_value from seq_feature_update_request) UNION (select last_value from seq_feature_deletion_request) UNION (select last_value from seq_notification_request) UNION (select last_value from seq_feature_copy_request)) as l), false);

alter table t_feature_request add constraint fk_feature_id foreign key (feature_id) references t_feature;

INSERT INTO t_feature_request SELECT id, 'CREATION', provider_id, session_owner, session_name, storages, errors, request_id, request_owner, state, feature, registration_date, request_date, step, priority, group_id, feature_id, override_previous_version, null, null, null, null, null FROM t_feature_creation_request;

INSERT INTO t_feature_request SELECT id, 'UPDATE', provider_id, null, null, null, errors, request_id, request_owner, state, feature, registration_date, request_date, step, priority, group_id, null, null, urn, null, null, null, null FROM t_feature_update_request;

INSERT INTO t_feature_request SELECT id, 'DELETION', null, null, null, null, errors, request_id, request_owner, state, null, registration_date, request_date, step, priority, group_id, null, null, urn, null, null, null, null FROM t_feature_deletion_request;

INSERT INTO t_feature_request SELECT id, 'NOTIFICATION', null, null, null, null, null, request_id, request_owner, state, null, registration_date, request_date, step, priority, null, null, null, urn, null, null, null, null FROM t_notification_request;

INSERT INTO t_feature_request SELECT id, 'COPY', null, null, null, null, null, request_id, request_owner, state, null, registration_date, request_date, step, priority, null, null, null, urn, storage, checksum, null, null FROM t_feature_copy_request;

-- Delete old tables
DROP TABLE t_feature_creation_request CASCADE;
DROP TABLE t_feature_update_request CASCADE;
DROP TABLE t_feature_deletion_request CASCADE;
DROP TABLE t_notification_request CASCADE;
DROP TABLE t_feature_copy_request CASCADE;

-- Delete t_feature_reference because it is now in another module
DROP TABLE t_feature_reference_request CASCADE;
DROP sequence seq_feature_reference_request;