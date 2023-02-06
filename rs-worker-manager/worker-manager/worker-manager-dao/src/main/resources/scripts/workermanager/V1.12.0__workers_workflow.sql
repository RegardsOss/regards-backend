-- alter table to add workflow functionality
alter table t_worker_conf
    ADD COLUMN content_type_out varchar(255);

alter table ta_worker_conf_content_types
    rename column content_type to content_type_in;
alter table ta_worker_conf_content_types
    rename to ta_worker_conf_content_types_in;

alter table t_workermanager_request
    add column step_number      int4 default 0,
    add column step_worker_type varchar(128);

-- add new table to save workflow of workers
create table t_workflow_config
(
    workflow_type varchar(255) not null primary key,
    steps         jsonb        not null
);
