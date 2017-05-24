/* Template */
create table t_template (id int8 not null, code varchar(255), content text, description varchar(255), subject varchar(255), primary key (id));
create table t_template_data (template_id int8 not null, dataStructure varchar(255), dataStructure_KEY varchar(255) not null, primary key (template_id, dataStructure_KEY));
alter table t_template drop constraint uk_template_code;
alter table t_template add constraint uk_template_code unique (code);
create sequence seq_template start 1 increment 50;
alter table t_template_data add constraint fk_template_data_template_id foreign key (template_id) references t_template;