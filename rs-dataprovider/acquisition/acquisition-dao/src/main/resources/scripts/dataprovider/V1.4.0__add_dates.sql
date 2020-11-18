-- Modify acq_file to scan directories according to their last dates
create table t_scan_dir_info (id int8 not null, scan_directory varchar(260), lastModificationDate timestamp, file_info_id int, primary key(id));
create sequence seq_scan_dir_info start 1 increment 50;
alter table t_scan_dir_info add constraint fk_file_info_id foreign key (file_info_id) references t_acq_file_info;

-- copy lastModificationDate into the new table t_scan_dir_info

-- drop last
alter table t_acq_file_info drop column lastModificationDate;
