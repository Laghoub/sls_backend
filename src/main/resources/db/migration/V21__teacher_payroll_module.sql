-- V21 - Paie des enseignants
-- V1 à V20 restent inchangées.

CREATE INDEX IF NOT EXISTS idx_payroll_period_dates ON payroll_period(start_date, end_date, status);
CREATE INDEX IF NOT EXISTS idx_salary_payment_status_date ON salary_payment(status, payment_date);
CREATE INDEX IF NOT EXISTS idx_payroll_employee_status ON payroll(employee_id, status);

CREATE TABLE payroll_email_outbox (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(300) NOT NULL,
    body_html TEXT NOT NULL,
    salary_payment_id BIGINT NOT NULL REFERENCES salary_payment(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ
);
CREATE INDEX idx_payroll_email_outbox_pending ON payroll_email_outbox(status, created_at);
CREATE UNIQUE INDEX uq_payroll_email_salary_recipient ON payroll_email_outbox(salary_payment_id, lower(recipient_email));

INSERT INTO permission(code,name,module,description,active)
SELECT v.code,v.name,'PAIE_ENSEIGNANT',v.description,TRUE FROM (VALUES
 ('PAIE_ENSEIGNANT_CONSULTER','Consulter la paie des enseignants','Consulter les calculs, bulletins et paiements des enseignants'),
 ('PAIE_ENSEIGNANT_CALCULER','Calculer la paie des enseignants','Calculer ou recalculer la rémunération des enseignants'),
 ('PAIE_ENSEIGNANT_PAYER','Payer les enseignants','Enregistrer un paiement de salaire et générer le reçu')
) v(code,name,description)
WHERE NOT EXISTS(SELECT 1 FROM permission p WHERE p.code=v.code);

INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code IN ('ADMIN','DIRECTEUR_FINANCIER')
  AND p.code IN ('PAIE_ENSEIGNANT_CONSULTER','PAIE_ENSEIGNANT_CALCULER','PAIE_ENSEIGNANT_PAYER')
  AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code='COMPTABLE'
  AND p.code IN ('PAIE_ENSEIGNANT_CONSULTER','PAIE_ENSEIGNANT_CALCULER','PAIE_ENSEIGNANT_PAYER')
  AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
