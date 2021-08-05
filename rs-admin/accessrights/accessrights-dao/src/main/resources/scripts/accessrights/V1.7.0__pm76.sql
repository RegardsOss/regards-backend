
alter table t_project_user
    add column firstname varchar(128);
alter table t_project_user
    add column lastname varchar(128);
alter table t_project_user
    add column created timestamp;
alter table t_project_user
    add column origin varchar(128);
alter table t_project_user
    add column max_quota int8;
alter table t_project_user
    add column current_quota int8;

create table ta_project_user_access_group
(
    project_user_id int8 not null,
    access_group    varchar(255)
);

alter table ta_project_user_access_group
    add constraint fk_ta_project_user_access_group_t_project_user foreign key (project_user_id) references t_project_user;