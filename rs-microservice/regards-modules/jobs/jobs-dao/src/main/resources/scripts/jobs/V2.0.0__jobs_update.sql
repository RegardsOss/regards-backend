-- Jobs
-- Clean
drop table t_job_info, t_job_parameters cascade;

-- Re-create
create table t_job_info (id uuid not null, class_name varchar(255), description text, expire_date timestamp, owner varchar(255), priority int4, result text, result_class_name varchar(255), estimate_completion timestamp, percent_complete int4, stacktrace text, start_date timestamp, status varchar(16), status_date timestamp, stop_date timestamp, primary key (id));
create table t_job_parameters (job_id uuid not null, class_name varchar(255), name varchar(100) not null, value text, primary key (job_id, name));
alter table t_job_parameters add constraint fk_job_param foreign key (job_id) references t_job_info;

create table t_task (id int8 not null, primary key (id));
create table ta_task_job_info (job_info_id uuid, task_id int8 not null, primary key (task_id));
create table ta_tasks_reliant_tasks (task_id int8 not null, reliant_task_id int8 not null, primary key (task_id, reliant_task_id));

create sequence seq_task start 1 increment 50;

alter table ta_task_job_info add constraint fk_job_info foreign key (job_info_id) references t_job_info;
alter table ta_task_job_info add constraint fk_task foreign key (task_id) references t_task;
alter table ta_tasks_reliant_tasks add constraint fk_reliant_task foreign key (reliant_task_id) references t_task;
alter table ta_tasks_reliant_tasks add constraint fk_task_2 foreign key (task_id) references t_task;