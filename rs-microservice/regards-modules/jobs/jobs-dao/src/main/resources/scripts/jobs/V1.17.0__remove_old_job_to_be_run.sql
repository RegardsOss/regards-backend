-- As in 1.17 version, to_be_run jobs are rescheduled, ensure first than there is no old job in this status to
-- avoid requeue old expired jobs.
DELETE FROM t_job_info WHERE status = 'TO_BE_RUN' AND status_date < NOW() - INTERVAL '7 DAY';