-- alter table to add workflow functionality
alter table t_worker_conf
    ADD COLUMN content_type_out varchar(255);

alter table ta_worker_conf_content_types
    rename column content_type to content_type_in;
alter table ta_worker_conf_content_types
    rename to ta_worker_conf_content_types_in;

alter table t_workermanager_request
    add column step int4 default 0;

-- add new table to save workflow of workers
create table t_workflow
(
    type         varchar(255) not null primary key,
    worker_types jsonb        not null
);
