-- FA #201156 - Add very useful index (Speed up by 14634 times) - requests was taking too much time
CREATE INDEX idx_data_file_order_id ON t_data_file USING btree (order_id);
