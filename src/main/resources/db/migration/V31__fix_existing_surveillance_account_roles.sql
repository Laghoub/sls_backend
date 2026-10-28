-- Correction ciblée des comptes de surveillants déjà actifs.
-- Ne modifie que les comptes qui ont exclusivement STAFF_ADMINISTRATIF.
-- Les comptes multi-rôles et les permissions personnalisées restent inchangés.
WITH candidates AS (
    SELECT ua.id AS user_id, r_target.id AS target_role_id, r_staff.id AS staff_role_id
    FROM user_account ua
    JOIN employee e ON e.person_id = ua.person_id
    JOIN job_position jp ON jp.id = e.position_id
    JOIN role r_target ON r_target.code = jp.code AND r_target.active = TRUE
    JOIN role r_staff ON r_staff.code = 'STAFF_ADMINISTRATIF'
    WHERE jp.code IN ('SURVEILLANT_GENERAL','SURVEILLANT_PRIMAIRE','SURVEILLANT_COLLEGE','SURVEILLANT_LYCEE')
      AND EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_account_id = ua.id AND ur.role_id = r_staff.id)
      AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_account_id = ua.id AND ur.role_id <> r_staff.id)
), inserted AS (
    INSERT INTO user_role (user_account_id, role_id, assigned_at)
    SELECT user_id, target_role_id, CURRENT_TIMESTAMP FROM candidates
    ON CONFLICT (user_account_id, role_id) DO NOTHING
    RETURNING user_account_id, role_id
)
DELETE FROM user_role ur
USING candidates c
WHERE ur.user_account_id = c.user_id AND ur.role_id = c.staff_role_id
  AND EXISTS (SELECT 1 FROM inserted i WHERE i.user_account_id = c.user_id AND i.role_id = c.target_role_id);
