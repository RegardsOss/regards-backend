-- Store ingest metadata
DROP INDEX idx_sip_processing;
DROP INDEX idx_sip_session;

alter table t_sip add column session_owner varchar(128) NOT NULL;
alter table t_sip add column session_name varchar(128) NOT NULL;
alter table t_sip RENAME COLUMN processing TO ingest_chain;
ALTER TABLE t_sip ADD COLUMN storages jsonb;

create index idx_sip_session_owner on t_sip (session_owner);
create index idx_sip_session on t_sip (session_name);
CREATE INDEX idx_sip_ingest_chain ON t_sip (ingest_chain);

create index idx_sip_state on t_sip (state);
create index idx_sip_providerId on t_sip (providerId);
create index idx_sip_ingest_date on t_sip (ingestDate);
create index idx_sip_version on t_sip (version);

-- ALTER TABLE fk_sip_session DROP CONSTRAINT IF EXISTS fk_sip_session;
alter table t_sip drop column session RESTRICT;
alter table t_sip drop column owner;
drop table t_sip_session;

-- Remove validation errors (transfer to requests)
alter table ta_sip_errors drop constraint fk_errors_sip_entity_id;
drop table ta_sip_errors;

-- Deletion request
create table t_deletion_request (id int8 not null, errors jsonb, request_id varchar(36) not null, state varchar(20) not null, deletion_mode int4 not null, providerIds jsonb, selection_mode int4 not null, session_name varchar(128) not null, session_owner varchar(128) not null, sipId varchar(128) not null, sipIds jsonb, job_info_id uuid, primary key (id));
create index idx_deletion_request_id on t_deletion_request (request_id);
create index idx_deletion_request_state on t_deletion_request (state);
alter table t_deletion_request add constraint uk_deletion_request_id unique (request_id);
alter table t_deletion_request add constraint uk_deletion_request_by_session unique (session_owner, session_name);
alter table t_deletion_request add constraint fk_req_job_info_id foreign key (job_info_id) references t_job_info;
create sequence seq_deletion_request start 1 increment 50;

-- Ingest request
create table t_ingest_request (id int8 not null, request_id varchar(36) NOT NULL, ingest_chain varchar(100) not null, session_name varchar(128) not null, session_owner varchar(128) not null, storages jsonb, state varchar(20) NOT NULL, errors jsonb, rawsip jsonb, job_info_id uuid, primary key (id));
CREATE INDEX idx_ingest_request_id on t_ingest_request (request_id);
CREATE INDEX idx_ingest_request_state ON t_ingest_request (state);
ALTER TABLE t_ingest_request ADD CONSTRAINT uk_ingest_request_id UNIQUE (request_id);
alter table t_ingest_request add constraint fk_req_job_info_id foreign key (job_info_id) references t_job_info;
create sequence seq_ingest_request start 1 increment 50;




