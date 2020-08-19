create table t_batch (
    id uuid not null,
    correlationId text,
    filesets jsonb,
    parameters jsonb,
    process text,
    tenant text,
    userName text,
    userRole text,
    primary key (id)
);
create table t_execution (
    id uuid not null,
    batch_id uuid,
    timeoutAfterMillis int8,
    currentStatus varchar(10),
    steps jsonb,
    fileParameters jsonb,
    process text,
    tenant text,
    userName text,
    version int8,
    created timestamptz,
    lastUpdated timestamptz,
    primary key (id)
);
alter table t_execution add constraint fk_batch foreign key (batch_id) references t_batch;
