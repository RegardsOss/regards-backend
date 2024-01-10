CREATE INDEX IF NOT EXISTS idx_model_att_att_id ON ta_model_att_att USING btree (id);
CREATE INDEX IF NOT EXISTS idx_model_att_att_model_id ON ta_model_att_att USING btree (model_id);
