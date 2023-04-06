CREATE
    TABLE
    t_email_requests(
               id int8 NOT NULL,
               attachment_name VARCHAR(100),
               attachment bytea,
               bcc_addrs VARCHAR(1000),
               cc_addrs VARCHAR(1000),
               from_addr VARCHAR(320),
               reply_to_addr VARCHAR(320),
               subject VARCHAR(78),
               text text,
               to_addrs VARCHAR(1000),
               nb_unsuccessfull_try smallint default 0,
               next_try_date TIMESTAMP NOT NULL,
               PRIMARY KEY(id)
);

CREATE
    SEQUENCE seq_email_request START 1 INCREMENT 50;

create index idx_next_try_date
    on t_email_requests (next_try_date);
