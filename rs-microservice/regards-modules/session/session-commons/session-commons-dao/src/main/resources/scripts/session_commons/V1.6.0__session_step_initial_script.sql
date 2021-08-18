CREATE TABLE t_snapshot_process (
    source              varchar(255)    NOT NULL,
    last_update_date    timestamp               ,
    job_id              uuid                    ,
    primary key (source)
);

CREATE TABLE t_session_step (
    step_id             varchar(255)    NOT NULL              ,
    source              varchar(255)    NOT NULL              ,
    session             varchar(255)    NOT NULL              ,
    type                varchar(100)    NOT NULL              ,
    input_related       int8            NOT NULL DEFAULT 0    ,
    output_related      int8            NOT NULL DEFAULT 0    ,
    errors              int8            NOT NULL DEFAULT 0    ,
    waiting             int8            NOT NULL DEFAULT 0    ,
    running             int8            NOT NULL DEFAULT 0    ,
    properties          jsonb           NOT NULL              ,
    last_update_date    timestamp       NOT NULL              ,
    primary key (step_id, source, session)
);