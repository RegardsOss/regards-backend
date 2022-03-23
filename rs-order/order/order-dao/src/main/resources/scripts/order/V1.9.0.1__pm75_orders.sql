CREATE INDEX idx_order_owner ON t_order USING btree (owner);
CREATE INDEX idx_order_creation_date ON t_order USING btree (creation_date);
CREATE INDEX idx_order_status ON t_order USING btree (status);
CREATE INDEX idx_order_creation_date_owner ON t_order USING btree (creation_date, owner);
CREATE INDEX idx_order_owner_status ON t_order USING btree (owner, status);