CREATE INDEX idx_request_aip_id ON t_request(aip_id);


CREATE INDEX idx_ta_ingest_request_aip_ingest_request_id ON ta_ingest_request_aip USING btree (ingest_request_id);
