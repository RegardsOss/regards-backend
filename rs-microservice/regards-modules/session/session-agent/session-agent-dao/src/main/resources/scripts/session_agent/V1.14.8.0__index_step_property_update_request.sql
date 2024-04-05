CREATE INDEX IF NOT EXISTS idx_gen_step_id_gen_source_gen_session
    ON t_step_property_update_request
    USING btree (gen_step_id, gen_source, gen_session);

CREATE INDEX IF NOT EXISTS idx_source_registration_date
    ON t_step_property_update_request
    USING btree (source, registration_date);
