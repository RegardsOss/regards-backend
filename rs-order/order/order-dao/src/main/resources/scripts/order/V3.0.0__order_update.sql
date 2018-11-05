-- Refactor Entity features
alter table t_data_file add column data_type varchar(32) default 'RAWDATA';
alter table t_data_file add column reference boolean default false;

-- Handle complex research with search engines like opensearch
alter table t_basket_dataset drop column opensearch_request;
alter table t_basket_ds_item drop column opensearch_request;
alter table t_basket_ds_item add column selection_request jsonb;
alter table t_dataset_task drop column opensearch_request;
alter table t_dataset_task add column selection_requests jsonb;



