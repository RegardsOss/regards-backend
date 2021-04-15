CREATE TABLE t_snapshot_process (
    source              varchar(255)    NOT NULL,
    last_update         timestamp               ,
    job_id              uuid                    ,
    primary key (source)
);

CREATE TABLE t_session_step (
    id                  int8            NOT NULL,
    step_id             varchar(255)    UNIQUE NOT NULL,
    source              varchar(255)    NOT NULL,
    session             varchar(255)    NOT NULL,
    type                varchar(100)    NOT NULL,
    in                  int8            NOT NULL,
    out                 int8            NOT NULL,
    state               varchar(100)    NOT NULL,
    properties          jsonb                   ,
    last_update         timestamp               ,
    errors              int8                    ,
    waiting             int8                    ,
    running             boolean                 ,
    primary key (step_id)
);

create sequence seq_session start 1 increment 50;