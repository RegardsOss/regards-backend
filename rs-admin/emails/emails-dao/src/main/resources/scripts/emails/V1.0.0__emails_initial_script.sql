create table t_email (id bigint not null, bcc varbinary(255), cc varbinary(255), _from varchar(255), replyTo varchar(255), sentDate timestamp, subject varchar(255), text longvarchar, _to varbinary(255), primary key (id));
create sequence seq_email start with 1 increment by 50;
