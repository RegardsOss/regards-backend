alter table t_job_info drop column description;
alter table t_job_info add column queued_date timestamp;
alter table t_job_info add column locked boolean not null default false;