alter table t_job_info drop column description;
alter table t_job_info add column queued_date timestamp;
alter table t_job_info add column locked boolean not null default false;

drop table ta_tasks_reliant_tasks cascade;

alter table t_task add column parent_id int8;
alter table t_task add constraint fk_task_parent_task foreign key (parent_id) references t_task;