-- V17 - Module Emploi du temps.
-- IMPORTANT : ne modifie aucune migration V1 à V16.

CREATE INDEX IF NOT EXISTS idx_schedule_entry_assignment_status
    ON schedule_entry (teaching_assignment_id, status);
CREATE INDEX IF NOT EXISTS idx_schedule_entry_day_status
    ON schedule_entry (day_of_week, status);
CREATE INDEX IF NOT EXISTS idx_schedule_entry_room_day_status
    ON schedule_entry (room_id, day_of_week, status)
    WHERE room_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_schedule_entry_validity
    ON schedule_entry (valid_from, valid_until);

INSERT INTO permission(code, name, module, description, active)
SELECT v.code, v.name, 'EMPLOI_DU_TEMPS', v.description, TRUE
FROM (VALUES
    ('EMPLOI_DU_TEMPS_CONSULTER', 'Consulter les emplois du temps', 'Consulter les emplois du temps par classe et par enseignant'),
    ('EMPLOI_DU_TEMPS_GERER', 'Gérer les emplois du temps', 'Créer, modifier, remplacer et désactiver les cours planifiés')
) AS v(code, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = v.code);

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.code = 'ADMIN'
  AND p.code IN ('EMPLOI_DU_TEMPS_CONSULTER','EMPLOI_DU_TEMPS_GERER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.code IN ('DIRECTEUR_PEDAGOGIQUE','SURVEILLANT_GENERAL')
  AND p.code IN ('EMPLOI_DU_TEMPS_CONSULTER','EMPLOI_DU_TEMPS_GERER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM role r CROSS JOIN permission p
WHERE r.code IN ('SURVEILLANT_PRIMAIRE','SURVEILLANT_COLLEGE','SURVEILLANT_LYCEE','ENSEIGNANT')
  AND p.code = 'EMPLOI_DU_TEMPS_CONSULTER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
