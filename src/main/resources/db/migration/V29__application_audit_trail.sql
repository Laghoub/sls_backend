-- V29 - Traçabilité / journal d'audit
-- audit_log existe déjà depuis V1 : cette migration l'enrichit sans perte de données.

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS username VARCHAR(150),
    ADD COLUMN IF NOT EXISTS user_last_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS user_first_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS user_roles VARCHAR(500),
    ADD COLUMN IF NOT EXISTS module VARCHAR(100),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS http_method VARCHAR(10),
    ADD COLUMN IF NOT EXISTS request_path VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS http_status INTEGER,
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(150);

UPDATE audit_log
SET module = 'HISTORIQUE'
WHERE module IS NULL OR TRIM(module) = '';

ALTER TABLE audit_log ALTER COLUMN module SET DEFAULT 'AUTRE';
ALTER TABLE audit_log ALTER COLUMN module SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_account ON audit_log (user_account_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_module ON audit_log (module);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_request_id ON audit_log (request_id);

INSERT INTO permission (code, name, module, description, active)
VALUES
('AUDIT_CONSULTER', 'Consulter la traçabilité', 'AUDIT',
 'Permet de consulter le journal de traçabilité de l''application', TRUE),
('AUDIT_EXPORTER', 'Exporter la traçabilité', 'AUDIT',
 'Permet d''exporter le journal de traçabilité', TRUE)
ON CONFLICT (code) DO NOTHING;
