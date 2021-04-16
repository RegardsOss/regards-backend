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
    primary key (id)
);

create sequence seq_step_property start 1 increment 50;