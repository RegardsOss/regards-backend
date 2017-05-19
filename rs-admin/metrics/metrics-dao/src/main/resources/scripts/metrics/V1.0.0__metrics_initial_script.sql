create table t_event_log (id int8 not null, caller varchar(255) not null, date varchar(32) not null, level varchar(32) not null, method varchar(255) not null, microService varchar(32) not null, msg varchar(2048) not null, userName varchar(255) not null, primary key (id));
create index idx_log_event on t_event_log (id);
create sequence seq_log_event start 1 increment 50;
