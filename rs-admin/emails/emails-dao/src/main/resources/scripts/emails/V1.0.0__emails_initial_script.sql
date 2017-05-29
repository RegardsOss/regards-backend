create table t_email (id bigint not null, bcc bytea, cc bytea, _from varchar(255), replyTo varchar(255), sentDate timestamp, subject varchar(255), text text, _to bytea, primary key (id));
create sequence seq_email start with 1 increment by 50;
