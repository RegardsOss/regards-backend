ALTER TABLE t_account
    ADD COLUMN origin varchar(128);

UPDATE t_account
SET origin = 'Regards'
WHERE external IS FALSE;

ALTER TABLE t_account
    DROP COLUMN external;

CREATE TABLE ta_account_project
(
    account_id int8 not null,
    project_id int8 not null,
    primary key (account_id, project_id)
);
ALTER TABLE ta_account_project
    ADD CONSTRAINT fk_account_project__account_id foreign key (account_id) references t_account;
ALTER TABLE ta_account_project
    ADD CONSTRAINT fk_account_project__project_id foreign key (project_id) references t_project;