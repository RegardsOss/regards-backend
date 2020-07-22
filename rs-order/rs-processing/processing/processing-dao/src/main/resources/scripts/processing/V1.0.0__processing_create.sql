create table t_batch (id uuid not null, correlationId text, filesets jsonb, parameters jsonb, process text, tenant text, userName text, userRole text, primary key (id));
create table t_execution (id uuid not null, fileParameters jsonb, timeoutAfterMillis int8, batch_id uuid, primary key (id));
create table t_execution_step (execution_id uuid not null, id SERIAL, message text, status varchar(255), time timestamptz);
alter table t_execution_step drop constraint if exists UK_ksk79ck8t2uqgdy6xdg3vd4f3;
alter table t_execution_step add constraint UK_ksk79ck8t2uqgdy6xdg3vd4f3 unique (id);
alter table t_execution add constraint fk_batch foreign key (batch_id) references t_batch;
alter table t_execution_step add constraint fk_execution_step_param foreign key (execution_id) references t_execution;
