alter table t_acq_processing_chain drop column period;
alter table t_acq_processing_chain add column period text;

alter table t_acq_processing_chain drop column session;
