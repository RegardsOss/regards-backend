/* Init script */
create table t_person (id int8 not null, name varchar(255), primary key (id));
create sequence seq_person start 1 increment 50;
