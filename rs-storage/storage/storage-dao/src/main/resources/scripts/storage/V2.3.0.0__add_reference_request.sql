create table t_file_reference_request (id int8 not null,
                                       creation_date timestamp,
                                       status varchar(255) not null,
                                       error_cause varchar(512),
                                       algorithm varchar(16) not null,
                                       checksum varchar(128) not null,
                                       fileName varchar(256) not null,
                                       fileSize int8, height int4, width int4,
                                       mime_type varchar(255) not null,
                                       type varchar(256),
                                       origin_url varchar(2048),
                                       storage varchar(128),
                                       storage_subdirectory varchar(2048),
                                       job_id varchar(255),
                                       session_name varchar(128),
                                       session_owner varchar(128),
                                       primary key (id));

create table ta_file_reference_request_owners (file_reference_request_id int8 not null,
                                               owner varchar(255),
                                               primary key (file_reference_request_id, owner));

create table ta_file_reference_request_group_ids (file_reference_request_id int8 not null,
                                                  group_id varchar(128) not null,
                                                  primary key (file_reference_request_id, group_id));

create index idx_file_reference_request on t_file_reference_request (storage, checksum);
create index idx_file_reference_request_checksum on t_file_reference_request (checksum);
create index idx_file_reference_request_storage on t_file_reference_request (storage);
create index idx_file_reference_request_status on t_file_reference_request (status);

create sequence seq_file_reference_request start 1 increment 50;

alter table ta_file_reference_request_owners
    add constraint fk_ta_file_reference_request_owners_t_file_reference_request
    foreign key (file_reference_request_id) references t_file_reference_request;

alter table ta_file_reference_request_group_ids
    add constraint fk_ta_reference_request_group_ids_t_file_reference_request
    foreign key (file_reference_request_id) references t_file_reference_request;
