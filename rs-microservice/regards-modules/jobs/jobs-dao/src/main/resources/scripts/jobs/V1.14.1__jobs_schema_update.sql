-- Fix creation of foreign key for table t_job_info to allow native delete cascade
alter table t_job_parameters drop constraint fk_job_param;
alter table t_job_parameters
    add constraint fk_job_param foreign key (job_id) references t_job_info ON DELETE CASCADE;

alter table ta_task_job_info drop constraint fk_job_info;
alter table ta_task_job_info
    add constraint fk_job_info foreign key (job_info_id) references t_job_info ON DELETE CASCADE;