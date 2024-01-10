CREATE INDEX IF NOT EXISTS idx_request_id ON t_request USING btree (id);

-- the index idx_sip_id is working on (provider_id, sipid, checksum) so its name is invalid
-- let's recreate it
DROP INDEX IF EXISTS idx_sip_id;
CREATE INDEX IF NOT EXISTS idx_sip_provider_id_sipid_checksum ON t_sip USING btree (provider_id, sipid, checksum);

CREATE INDEX IF NOT EXISTS idx_sip_id ON t_sip USING btree (id);
