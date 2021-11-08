create table t_workermanager_request (
    id int8 not null,
    content bytea not null,
    content_type varchar(255) not null,
    creation_date timestamp not null,
    dispatched_worker_type varchar(255),
    request_id varchar(255) not null,
    session varchar(255) not null,
    source varchar(255) not null,
    status varchar(255) not null,
    error varchar(255),
    primary key (id)
);
create index idx_worker_request_id
    on t_workermanager_request (request_id);

create index idx_worker_request_content_type
    on t_workermanager_request (content_type);

alter table if exists t_workermanager_request drop constraint if exists uk_t_workermanager_request_requestid;
alter table if exists t_workermanager_request add constraint uk_t_workermanager_request_requestid unique (request_id);

create sequence worker_request_sequence start 1 increment 50;
