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
    session_step_id     int8                    ,
    primary key (id)
);

CREATE sequence seq_step_property start 1 increment 50;

ALTER TABLE t_step_property_update_request ADD CONSTRAINT fk_session_step_id FOREIGN KEY (session_step_id) REFERENCES
t_session_step;

