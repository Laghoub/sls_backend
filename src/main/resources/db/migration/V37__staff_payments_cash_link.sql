-- Separate staff salary disbursements from teacher salary payments; keep existing Flyway migrations immutable.
ALTER TABLE staff_pay_payment ADD COLUMN cash_register_session_id BIGINT REFERENCES cash_register_session(id);
ALTER TABLE staff_pay_payment ADD COLUMN created_by BIGINT REFERENCES user_account(id);
ALTER TABLE cash_movement ADD COLUMN staff_pay_payment_id BIGINT REFERENCES staff_pay_payment(id);
CREATE UNIQUE INDEX ux_cash_movement_staff_payment ON cash_movement(staff_pay_payment_id) WHERE staff_pay_payment_id IS NOT NULL;
