-- V18 - Module Assiduité des enseignants.
-- IMPORTANT : ne modifie aucune migration V1 à V17.

CREATE INDEX IF NOT EXISTS idx_class_session_date_time_slot
    ON class_session (session_date, time_slot_id);

CREATE INDEX IF NOT EXISTS idx_class_session_teacher_date
    ON class_session (teacher_id, session_date);

CREATE INDEX IF NOT EXISTS idx_teacher_attendance_recorded_at
    ON teacher_attendance (recorded_at);

INSERT INTO permission(code, name, module, description, active)
SELECT v.code, v.name, 'ASSIDUITE_ENSEIGNANT', v.description, TRUE
FROM (VALUES
    ('ASSIDUITE_ENSEIGNANT_CONSULTER', 'Consulter l’assiduité des enseignants', 'Consulter les présences, absences et retards des enseignants'),
    ('ASSIDUITE_ENSEIGNANT_SAISIR', 'Saisir l’assiduité des enseignants', 'Enregistrer et modifier une saisie d’assiduité non encore validée'),
    ('ASSIDUITE_ENSEIGNANT_VALIDER', 'Valider l’assiduité des enseignants', 'Valider, rejeter ou rouvrir une saisie d’assiduité enseignant')
) AS v(code, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = v.code);

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.code = 'ADMIN'
  AND p.code IN ('ASSIDUITE_ENSEIGNANT_CONSULTER','ASSIDUITE_ENSEIGNANT_SAISIR','ASSIDUITE_ENSEIGNANT_VALIDER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.code IN ('DIRECTEUR_PEDAGOGIQUE','SURVEILLANT_GENERAL')
  AND p.code IN ('ASSIDUITE_ENSEIGNANT_CONSULTER','ASSIDUITE_ENSEIGNANT_SAISIR','ASSIDUITE_ENSEIGNANT_VALIDER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.code IN ('SURVEILLANT_PRIMAIRE','SURVEILLANT_COLLEGE','SURVEILLANT_LYCEE')
  AND p.code IN ('ASSIDUITE_ENSEIGNANT_CONSULTER','ASSIDUITE_ENSEIGNANT_SAISIR')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
