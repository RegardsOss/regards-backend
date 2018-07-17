create index idx_aip_aip_session on t_aip (session);
alter table t_aip add constraint fk_aip_session_aip_session foreign key (session) references t_aip_session;
