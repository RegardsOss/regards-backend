-- Add generation and submission retry properties
alter table t_acq_processing_chain add column generation_retry_enabled boolean not null DEFAULT false;
alter table t_acq_processing_chain add column submission_retry_enabled boolean not null DEFAULT true;