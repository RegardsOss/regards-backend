create table t_project (id int8 not null, label varchar(256) not null, description text, host varchar(255), icon varchar(255), accessible boolean, deleted boolean, public boolean, licenceLink text, name varchar(30), crs VARCHAR (32) DEFAULT 'WGS_84', pole_managed boolean DEFAULT FALSE,primary key (id));
create table t_project_connection (id int8 not null, state VARCHAR(50) not null, driverClassName varchar(200) not null, microservice varchar(50) not null, password varchar(255) not null, url varchar(255) not null, userName varchar(30) not null, project_id int8, cause VARCHAR(255), primary key (id));
alter table t_project add constraint uk_project_name unique (name);
alter table t_project_connection add constraint uk_project_connection_project_microservice unique (project_id, microservice);
create sequence seq_project start 1 increment 50;
create sequence seq_project_connection start 1 increment 50;
alter table t_project_connection add constraint fk_project_connection foreign key (project_id) references t_project;
