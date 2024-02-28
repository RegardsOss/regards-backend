create table if not exists ta_file_cache_request_group_id
(
    file_cache_request_id int8 not null,
    group_id           varchar(255)
);

alter table ta_file_cache_request_group_id
    add constraint fk_ta_file_cache_request_group_id_file_cache_request_id foreign key (file_cache_request_id) references
        t_file_cache_request;

insert into ta_file_cache_request_group_id (file_cache_request_id, group_id) select id, group_id from
                                                                                                     t_file_cache_request;

drop index if exists idx_file_cache_request_grp;

alter table t_file_cache_request
    drop column IF EXISTS group_id;

alter table t_file_cache_request drop constraint uk_t_file_cache_request_checksum;