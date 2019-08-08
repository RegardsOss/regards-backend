-- Store ingest metadata
DROP INDEX idx_sip_processing;
DROP INDEX idx_sip_session;

alter table t_sip add column session_owner varchar(128) NOT NULL;
alter table t_sip add column client_session varchar(128) NOT NULL;
alter table t_sip RENAME COLUMN processing TO ingest_chain;
ALTER TABLE t_sip ADD COLUMN storages jsonb;

create index idx_sip_session_owner on t_sip (session_owner);
create index idx_sip_session on t_sip (session);
CREATE INDEX idx_sip_ingest_chain ON t_sip (ingest_chain);

create index idx_sip_state on t_sip (state);
create index idx_sip_providerId on t_sip (providerId);
create index idx_sip_ingest_date on t_sip (ingestDate);
create index idx_sip_version on t_sip (version);

ALTER TABLE fk_sip_session DROP CONSTRAINT fk_sip_session;
alter table t_sip drop column session RESTRICT;
drop table t_sip_session;
