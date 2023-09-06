-- DELIVERY REQUEST TABLE
create table t_delivery_request
(
    id                      int8         not null primary key,
    correlation_id          varchar(255) not null,
    user_name               varchar(128) not null,
    order_id                int8,
    total_sub_orders        int4,
    status                  varchar(50)  not null,
    creation_date           timestamp    not null default CURRENT_TIMESTAMP,
    status_date             timestamp    not null default CURRENT_TIMESTAMP,
    expiry_date             timestamp    not null,
    origin_request_app_id   varchar(255) not null,
    origin_request_priority int4         not null default 1,
    error_type              varchar(100),
    error_cause             text,
    version                 int4         not null default 0,
    constraint delivery_unique unique (correlation_id)
);

create sequence seq_delivery_request start 1 increment 50;

create index idx_delivery_correlation_id on t_delivery_request (correlation_id varchar_ops);
create index idx_delivery_requests_owner on t_delivery_request (user_name varchar_ops);
create index idx_delivery_requests_status on t_delivery_request (status);
create index idx_delivery_requests_status_date on t_delivery_request (status_date);
create index idx_delivery_requests_expiry_date on t_delivery_request (expiry_date);


-- ASSOCIATION TABLE BETWEEN DELIVERY REQUEST / JOB_INFO
create table ta_delivery_request_job_info
(
    id          int8 not null primary key,
    delivery_id int8 references t_delivery_request (id),
    job_id      uuid references t_job_info (id),
    constraint delivery_job_unique unique (delivery_id, job_id)
);

create sequence seq_delivery_and_job start 1 increment 50;
