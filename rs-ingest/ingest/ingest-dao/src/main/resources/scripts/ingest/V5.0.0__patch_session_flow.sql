-- Store ingest metadata
DROP INDEX idx_sip_processing;
DROP INDEX idx_sip_session;

alter table t_sip add column client_id varchar(128) NOT NULL;
alter table t_sip add column client_session varchar(128) NOT NULL;
alter table t_sip RENAME COLUMN processing TO ingest_chain;
ALTER TABLE t_sip ADD COLUMN storages jsonb;

create index idx_sip_client_id on t_sip (client_id);
create index idx_sip_client_session on t_sip (client_session);
CREATE INDEX idx_sip_ingest_chain ON t_sip (ingest_chain);

ALTER TABLE fk_sip_session DROP CONSTRAINT fk_sip_session;
alter table t_sip drop column session RESTRICT;
drop table t_sip_session;
