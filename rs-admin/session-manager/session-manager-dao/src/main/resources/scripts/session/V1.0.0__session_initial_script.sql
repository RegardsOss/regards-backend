create table t_session (id int8 not null, creation_date timestamp not null, last_update_date timestamp not null, life_cycle jsonb, name varchar(128) not null, source varchar(128) not null, state varchar(16) not null, is_latest boolean, primary key (id));
create index idx_session_name on t_session (name, source);
create index idx_session_state on t_session (state);
create index idx_session_creation_date on t_session (creation_date);
create index idx_session_is_latest on t_session (is_latest);
create index idx_last_update_date on t_session (last_update_date);
alter table t_session add constraint uk_session_source_name unique (name, source);
create sequence seq_session start 1 increment 50;
