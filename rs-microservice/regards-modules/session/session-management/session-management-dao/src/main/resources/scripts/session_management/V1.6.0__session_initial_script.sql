-- Session
CREATE TABLE t_session (
    id                  int8            NOT NULL              ,
    source              varchar(255)    NOT NULL              ,
    name                varchar(255)    NOT NULL              ,
    creation_date       timestamp       NOT NULL              ,
    last_update_date    timestamp                             ,
    steps               int8            NOT NULL              ,
    running             boolean         NOT NULL DEFAULT false,
    error               boolean         NOT NULL DEFAULT false,
    waiting             boolean         NOT NULL DEFAULT false,
    primary key (id)
);

ALTER TABLE t_session ADD CONSTRAINT fk_session_step_aggregation FOREIGN KEY (steps) REFERENCES t_session_step;

CREATE sequence seq_session start 1 increment 50;

-- Source

CREATE TABLE t_source_step_aggregation (
    source              varchar(255)    NOT NULL              ,
    type                varchar(100)    NOT NULL              ,
    total_id            int8            NOT NULL DEFAULT 0    ,
    total_out           int8            NOT NULL DEFAULT 0    ,
    errors              int8            NOT NULL DEFAULT 0    ,
    waiting             int8            NOT NULL DEFAULT 0    ,
    running             boolean         NOT NULL DEFAULT false,
    primary key (source)
);

CREATE TABLE t_source (
    name                varchar(255)    NOT NULL              ,
    nb_sessions         int8            NOT NULL DEFAULT 0    ,
    steps               varchar(255)    NOT NULL              ,
    last_update_date    timestamp                             ,
    errors              boolean         NOT NULL DEFAULT false,
    waiting             boolean         NOT NULL DEFAULT false,
    running             boolean         NOT NULL DEFAULT false,
    primary key (name)
);

ALTER TABLE t_source ADD CONSTRAINT fk_source_step_aggregation FOREIGN KEY (steps) REFERENCES t_source_step_aggregation;


