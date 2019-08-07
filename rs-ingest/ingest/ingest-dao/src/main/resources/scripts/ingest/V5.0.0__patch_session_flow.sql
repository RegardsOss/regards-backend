alter table t_sip add column session_name varchar(128);
alter table t_sip add column session_source varchar(128);
create index idx_sip_session_source on t_sip (session_source);
create index idx_sip_session_name on t_sip (session_name);
drop index if exists fk_sip_session;
alter table t_sip drop column session RESTRICT;
drop table t_sip_session;
