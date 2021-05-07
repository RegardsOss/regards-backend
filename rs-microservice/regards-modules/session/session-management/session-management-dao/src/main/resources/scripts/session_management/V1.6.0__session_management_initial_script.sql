-- Session
CREATE TABLE t_session_manager (
    id                  int8            NOT NULL              ,
    name                varchar(255)    NOT NULL              ,
    source              varchar(255)    NOT NULL              ,
    creation_date       timestamp       NOT NULL              ,
    last_update_date    timestamp                             ,
    steps               jsonb           NOT NULL              ,
    running             boolean         NOT NULL DEFAULT false,
    errors              boolean         NOT NULL DEFAULT false,
    waiting             boolean         NOT NULL DEFAULT false,
    primary key (id)
);

CREATE sequence seq_session_manager start 1 increment 50;

-- Source

CREATE TABLE t_source_manager (
    name                varchar(255)    NOT NULL              ,
    nb_sessions         int8            NOT NULL DEFAULT 0    ,
    last_update_date    timestamp                             ,
    errors              boolean         NOT NULL DEFAULT false,
    waiting             boolean         NOT NULL DEFAULT false,
    running             boolean         NOT NULL DEFAULT false,
    primary key (name)
);

CREATE TABLE t_source_step_aggregation (
    id                  int8            NOT NULL              ,
    type                varchar(100)    NOT NULL              ,
    total_in            int8            NOT NULL DEFAULT 0    ,
    total_out           int8            NOT NULL DEFAULT 0    ,
    errors              int8            NOT NULL DEFAULT 0    ,
    waiting             int8            NOT NULL DEFAULT 0    ,
    running             int8            NOT NULL DEFAULT 0    ,
    source_name         varchar(255)                          ,
    primary key (id)
);

ALTER TABLE t_source_step_aggregation ADD CONSTRAINT fk_source_step_aggregation FOREIGN KEY (source_name) REFERENCES
t_source_manager;

CREATE sequence seq_source_agg start 1 increment 50;



