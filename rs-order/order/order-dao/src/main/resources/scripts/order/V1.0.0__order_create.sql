-- Order
-- Create
create table t_basket (id int8 not null, owner varchar(100) not null, primary key (id));
create table t_basket_dataset (id int8 not null, dataset_ip_id varchar(128) not null, dataset_label varchar(128) not null, files_count int4, files_size int8, objects_count int4, basket_id int8, primary key (id));
create table t_basket_ds_item (basket_dataset_id int8 not null, date timestamp not null, files_count int4, files_size int8, objects_count int4, selection_request jsonb, primary key (basket_dataset_id, date));
create table t_data_file (id int8 not null, checksum varchar(128), checksum_algo varchar(10), data_objects_ip_id varchar(128), mime_type varchar(64), name varchar(255), online boolean, order_id int8, size int8, state varchar(16), url text, files_task_id int8, download_error_reason text, data_type varchar(32) default 'RAWDATA', reference boolean default false, primary key (id));
create table t_dataset_task (dataset_ip_id varchar(128) not null, dataset_label varchar(128) not null, files_count int4, files_size int8, objects_count int4, selection_requests jsonb, processing_service text, id int8 not null, order_id int8, primary key (id));
create table t_files_task (id int8 not null, ended boolean, order_id int8, owner varchar(100) not null, waiting_for_user boolean, primary key (id));

create table t_order (id int8 not null, available_count int4 not null, avail_count_update_date timestamp, creation_date timestamp not null, expiration_date timestamp, files_in_error int4 not null, owner varchar(100) not null, percent_complete int4 not null, status varchar(20) not null, status_date timestamp not null, waiting_for_user boolean not null, url text, primary key (id));

alter table t_basket add constraint uk_basket_owner unique (owner);
create index data_file_idx on t_data_file (checksum, order_id, state, data_objects_ip_id);
create sequence seq_basket start 1 increment 50;
create sequence seq_data_file start 1 increment 50;
create sequence seq_ds_items_sel start 1 increment 50;
create sequence seq_order start 1 increment 50;

alter table t_basket_dataset add constraint fk_dataset_selection foreign key (basket_id) references t_basket;
alter table t_basket_ds_item add constraint fk_items_selection foreign key (basket_dataset_id) references t_basket_dataset;
alter table t_data_file add constraint fk_files_task foreign key (files_task_id) references t_files_task;
alter table t_dataset_task add constraint fk_task_id foreign key (id) references t_task;
alter table t_dataset_task add constraint fk_order foreign key (order_id) references t_order;
alter table t_files_task add constraint fk_task_id foreign key (id) references t_task;

