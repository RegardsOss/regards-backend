create table t_project (id int8 not null, description varchar(200), host varchar(255), icon varchar(255), accessible boolean, deleted boolean, public boolean, licenceLink text, name varchar(30), primary key (id));
create table t_project_connection (id int8 not null, driverClassName varchar(200) not null, enabled boolean not null, microservice varchar(50) not null, password varchar(255) not null, url varchar(255) not null, userName varchar(30) not null, project_id int8, primary key (id));
alter table t_project add constraint uk_project_name unique (name);
alter table t_project_connection add constraint uk_project_connection_project_microservice unique (project_id, microservice);
create sequence seq_project start 1 increment 50;
create sequence seq_project_connection start 1 increment 50;
alter table t_project_connection add constraint fk_project_connection foreign key (project_id) references t_project;
