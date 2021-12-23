create table t_workermanager_request
(
    id                     int8         not null,
    content                bytea        not null,
    content_type           varchar(255) not null,
    creation_date          timestamp    not null,
    dispatched_worker_type varchar(255),
    request_id             varchar(255) not null,
    session                varchar(255) not null,
    source                 varchar(255) not null,
    status                 varchar(255) not null,
    error                  text,
    primary key (id)
);

create table t_worker_conf
(
    id          int8 not null,
    worker_type varchar(128),
    primary key (id)
);
create table ta_worker_conf_content_types
(
    worker_conf_id int8 not null,
    content_type   varchar(255)
);
create index idx_worker_request_id
    on t_workermanager_request (request_id);

create index idx_worker_request_content_type
    on t_workermanager_request (content_type);

alter table if exists t_workermanager_request
    drop constraint if exists uk_t_workermanager_request_requestid;
alter table if exists t_workermanager_request
    add constraint uk_t_workermanager_request_requestid unique (request_id);

create sequence worker_request_sequence start 1 increment 50;

alter table t_worker_conf
    add constraint uk_worker_conf_worker_type unique (worker_type);
alter table ta_worker_conf_content_types
    add constraint uk_worker_conf_content_type unique (content_type);
create sequence seq_worker_conf start 1 increment 50;
alter table ta_worker_conf_content_types
    add constraint fk_worker_conf_content_type foreign key (worker_conf_id) references t_worker_conf;
