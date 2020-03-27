create table t_template (id int8 not null, code varchar(100) not null, content text, primary key (id));
alter table t_template add constraint uk_template_code unique (code);
create sequence seq_template start 1 increment 50;
