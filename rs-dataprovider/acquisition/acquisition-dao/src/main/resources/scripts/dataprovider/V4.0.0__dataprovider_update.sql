-- Remove unnecessary submission job reference
alter table t_acquisition_product drop constraint fk_sip_submission_job_info_id;
alter table t_acquisition_product drop column sip_submission_job_info_id;
-- Remove unnecessary submission retry properties
alter table t_acq_processing_chain drop column submission_retry_enabled;