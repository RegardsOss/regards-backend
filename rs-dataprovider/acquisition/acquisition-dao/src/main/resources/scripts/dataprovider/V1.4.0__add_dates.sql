-- Modify acq_file to scan directories according to their last dates
create table t_scan_dir_info (id int8 not null, scan_directory varchar(260), lastModificationDate timestamp, primary key(id));
create sequence seq_scan_dir_info start 1 increment 50;
alter table t_acq_file_info add scan_dir_id int8;
alter table t_acq_file_info drop column lastModificationDate;
--alter table t_scan_dir_info add constraint fk_scan_dir_id foreign key (scan_dir_id) references t_acq_file_info;
