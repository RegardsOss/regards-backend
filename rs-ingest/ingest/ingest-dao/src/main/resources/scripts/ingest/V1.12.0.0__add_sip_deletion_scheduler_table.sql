CREATE TABLE t_sip_deletion_scheduler
(
    id                  INT PRIMARY KEY NOT NULL,
    last_scheduled_date TIMESTAMP       NOT NULL
);
CREATE SEQUENCE seq_sip_deletion_scheduler START 1 INCREMENT 50;