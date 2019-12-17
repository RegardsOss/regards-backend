-- Store ingest metadata
DROP INDEX idx_sip_processing;
DROP INDEX idx_sip_session;

alter table t_sip add column session_owner varchar(128) NOT NULL;
alter table t_sip add column session_name varchar(128) NOT NULL;
alter table t_sip add column ip_type varchar(20) NOT NULL;

alter table t_sip RENAME column providerid to provider_id;
alter table t_sip DROP COLUMN processing;
alter table t_sip RENAME COLUMN ingestDate TO creation_date;
alter table t_sip RENAME COLUMN lastUpdateDate TO last_update;

ALTER TABLE t_sip ADD COLUMN tags jsonb;
ALTER TABLE t_sip ADD COLUMN storages jsonb;
ALTER TABLE t_sip ADD COLUMN categories jsonb;
ALTER TABLE t_sip ADD COLUMN errors jsonb;

create index idx_sip_session_owner on t_sip (session_owner);
create index idx_sip_session on t_sip (session_name);
create index idx_sip_ip_type on t_sip (ip_type);
CREATE INDEX idx_sip_storage ON t_sip USING gin (storages);


create index idx_sip_state on t_sip (state);
create index idx_sip_providerId on t_sip (provider_id);
create index idx_sip_creation_date on t_sip (creation_date);
create index idx_sip_version on t_sip (version);


-- Propagate ingest metadata to AIP for search purpose

alter table t_aip add column session_owner varchar(128) NOT NULL;
alter table t_aip add column session_name varchar(128) NOT NULL;
alter table t_aip add column ip_type varchar(20) NOT NULL;
alter table t_aip ADD COLUMN checksum varchar(128);
ALTER TABLE t_aip ADD COLUMN storages jsonb;
ALTER TABLE t_aip ADD COLUMN errors jsonb;
ALTER TABLE t_aip ADD COLUMN manifest_locations jsonb;



ALTER TABLE t_aip ADD COLUMN provider_id varchar(100) NOT NULL;
ALTER TABLE t_aip ADD COLUMN last_update TIMESTAMP NOT NULL;
alter table t_aip add column categories jsonb not null;
alter table t_aip add column tags jsonb;
alter table t_aip drop column error_message;
alter table t_aip rename column aipId to aip_id;

CREATE INDEX idx_search_aip ON t_aip (session_owner, session_name, state, last_update, ip_type);

CREATE INDEX idx_aip_storage ON t_aip USING gin (storages);
CREATE INDEX idx_aip_provider_id ON t_aip (provider_id);
CREATE INDEX idx_aip_tags ON t_aip USING gin (tags);
CREATE INDEX idx_aip_categories ON t_aip USING gin (categories);

-- ALTER TABLE fk_sip_session DROP CONSTRAINT IF EXISTS fk_sip_session;
alter table t_sip drop column session RESTRICT;
alter table t_sip drop column owner;
drop table t_sip_session;

-- Remove validation errors (transfer to requests)
alter table ta_sip_errors drop constraint fk_errors_sip_entity_id;
drop table ta_sip_errors;

-- Ingest request

create table t_request (
    dtype varchar(32) not null,
    id int8 not null,
    creation_date timestamp not null,
    errors jsonb,
    provider_id varchar(128),
    remote_step_deadline timestamp,
    remote_step_group_ids jsonb,
    session_name varchar(128),
    session_owner varchar(128),
    payload jsonb,
    state varchar(50),
    sip_id varchar(128),
    job_info_id uuid,
    aip_id int8,
    update_task_id int8,
    primary key (id)
);
create sequence seq_request start 1 increment 50;

create index idx_request_search on t_request (session_owner, session_name, provider_id);
create INDEX idx_request_remote_step_group_ids on t_request using gin (remote_step_group_ids);

alter table t_request add constraint fk_req_job_info_id foreign key (job_info_id) references t_job_info;
alter table t_request add constraint fk_update_request_aip foreign key (aip_id) references t_aip;

--
-- Join table to link AIP to ingest request
create table ta_ingest_request_aip (
  ingest_request_id int8 not null,
  aip_id int8 not null,
  primary key (ingest_request_id,aip_id)
);
alter table ta_ingest_request_aip add constraint uk_ingest_request_aip_aip_id unique (aip_id);
alter table ta_ingest_request_aip add constraint fk_ingest_request_aip_aip_id foreign key (aip_id) references t_aip;
alter table ta_ingest_request_aip add constraint fk_ingest_request_aip_request_id foreign key (ingest_request_id) references t_request;

create table t_update_task (
    dtype varchar(30) not null,
    id int8 not null,
    state varchar(255),
    type varchar(255),
    payload jsonb,
    primary key (id)
);
create sequence seq_aip_update_task start 1 increment 50;
alter table t_request add constraint fk_update_request_update_task_id foreign key (update_task_id) references t_update_task;

