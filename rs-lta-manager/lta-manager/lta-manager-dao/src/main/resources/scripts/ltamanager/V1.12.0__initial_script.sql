create table t_submission_requests
(
    request_id    varchar(36)  not null,
    owner         varchar(128) not null,
    session       varchar(128) not null,
    status        varchar(50)  not null,
    status_date   timestamp    not null default CURRENT_TIMESTAMP,
    creation_date timestamp    not null default CURRENT_TIMESTAMP,
    model         varchar(32)  not null,
    datatype      varchar(255) not null,
    store_path    varchar(255) not null,
    replace_mode  boolean      not null default false,
    product       jsonb        not null,
    message       text,
    origin_urn    varchar(255),
    primary key (request_id)
);


create index idx_submission_requests_owner_session on t_submission_requests (owner varchar_pattern_ops, session varchar_pattern_ops);
create index idx_submission_requests_status on t_submission_requests (status);
create index idx_submission_requests_status_date on t_submission_requests (status_date);
