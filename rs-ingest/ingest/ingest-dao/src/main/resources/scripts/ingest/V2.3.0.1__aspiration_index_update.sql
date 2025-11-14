-- Create a new index for OAIS products to improve performance of aspirations
CREATE INDEX IF NOT EXISTS idx_aip_last_update_id ON t_aip USING btree (last_update, id);
