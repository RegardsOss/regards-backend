create table t_worker_conf
(
    id   int8 not null,
    worker_type varchar(128),
    primary key (id)
);
create table ta_worker_conf_content_types
(
    worker_conf_id int8 not null,
    content_type   varchar(255)
);
alter table t_worker_conf
    add constraint uk_worker_conf_worker_type unique (worker_type);
alter table ta_worker_conf_content_types
    add constraint uk_worker_conf_content_type unique (content_type);
create sequence seq_storage_location_conf start 1 increment 50;
alter table ta_worker_conf_content_types
    add constraint fk_worker_conf_content_type foreign key (worker_conf_id) references t_worker_conf;
