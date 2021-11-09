create table t_tinyurl (id int8 not null, class varchar(255) not null, context jsonb not null, expirationDate timestamp not null, uuid varchar(36) not null, primary key (id));
create index idx_tinyurl_uuid on t_tinyurl (uuid);
alter table if exists t_tinyurl add constraint uk_tinyurl_uuid unique (uuid);
create sequence seq_tinyurl start 1 increment 50;