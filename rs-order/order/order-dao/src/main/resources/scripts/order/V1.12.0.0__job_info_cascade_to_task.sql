ALTER TABLE ta_task_job_info
    DROP CONSTRAINT fk_job_info,
    ADD CONSTRAINT fk_job_info
        FOREIGN KEY (job_info_id)
            REFERENCES t_job_info (id)
            ON DELETE CASCADE;

ALTER TABLE t_basket_dataset
    ADD COLUMN file_selection_description jsonb;

ALTER TABLE t_order
    ADD COLUMN message varchar(255),
    ADD COLUMN correlation_id varchar(100);
