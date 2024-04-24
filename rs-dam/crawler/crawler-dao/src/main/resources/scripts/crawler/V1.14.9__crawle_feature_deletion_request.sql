create sequence if not exists seq_deletion_request start 1 increment 50;

create table if not exists t_entity_deletion_request
(
    id                    int8 not null,
    entity_id                    varchar(255) not null,
    primary key (id)
);
