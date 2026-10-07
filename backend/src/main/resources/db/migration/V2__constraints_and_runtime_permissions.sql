CREATE INDEX idx_session_user ON auth_session(user_id);
CREATE INDEX cart_customer ON cart_line(user_id);
CREATE INDEX reset_expiry ON reset_token(expires_at);
ALTER TABLE product ADD CONSTRAINT reorder_values CHECK (reorder_point>=0 AND reorder_quantity>0);
ALTER TABLE product ADD CONSTRAINT computer_specs CHECK (category='ACCESSORY' OR (cpu IS NOT NULL AND ram IS NOT NULL AND storage IS NOT NULL AND os IS NOT NULL));
REVOKE ALL ON flyway_schema_history FROM np_app;
