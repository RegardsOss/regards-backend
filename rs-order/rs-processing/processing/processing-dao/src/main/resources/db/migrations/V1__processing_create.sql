create table t_batch (
    id uuid not null,
    process_business_id uuid,
    process_name text,
    correlation_id text,
    filesets jsonb,
    parameters jsonb,
    process text,
    tenant text,
    user_email text,
    user_role text,
    primary key (id)
);
create table t_execution (
    id uuid not null,
    batch_id uuid,
    timeout_after_millis int8,
    current_status varchar(10),
    steps jsonb,
    file_parameters jsonb,
    process_business_id uuid,
    process_name text,
    tenant text,
    user_email text,
    version int8,
    created timestamptz,
    last_updated timestamptz,
    primary key (id)
);
alter table t_execution add constraint fk_batch foreign key (batch_id) references t_batch;
