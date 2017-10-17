-- Template
-- Clean
drop table t_template, t_template_data cascade;
drop sequence seq_template;
-- Re-create
create table t_template (id int8 not null, code varchar(100) not null, content text, description varchar(100), subject varchar(100), primary key (id));
create table t_template_data (template_id int8 not null, value varchar(128), name varchar(48) not null, primary key (template_id, name));
alter table t_template add constraint uk_template_code unique (code);
alter table t_template_data add constraint fk_template_data_template_id foreign key (template_id) references t_template;
