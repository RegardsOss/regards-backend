CREATE TABLE t_step_property_update_request (
    id                  int8            NOT NULL,
    step_id             varchar(255)    NOT NULL,
    source              varchar(255)    NOT NULL,
    session             varchar(255)    NOT NULL,
    date                timestamp       NOT NULL,
    step_type           varchar(100)    NOT NULL,
    state               varchar(100)    NOT NULL,
    property            varchar(255)    NOT NULL,
    value               varchar(255)    NOT NULL,
    type                varchar(100)    NOT NULL,
    input_related       boolean         NOT NULL,
    output_related      boolean         NOT NULL,
    gen_step_id         varchar(255)            ,
    gen_source          varchar(255)            ,
    gen_session         varchar(255)            ,
    primary key (id)
);

CREATE sequence seq_step_property start 1 increment 50;

ALTER TABLE t_step_property_update_request ADD CONSTRAINT fk_session_step_id FOREIGN KEY (gen_step_id, gen_source,
gen_session) REFERENCES t_session_step(step_id, source, session);

