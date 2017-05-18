/* Init script */
create table t_user (id int8 not null, name varchar(255), primary key (id));
create sequence seq_user start 1 increment 50;
