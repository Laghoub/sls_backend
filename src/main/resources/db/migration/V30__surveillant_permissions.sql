-- Permissions de base des surveillants; les DENY individuels restent prioritaires.
-- Aucun droit de modification des référentiels, finance ou administration.
INSERT INTO role_permission (role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code IN ('SURVEILLANT_PRIMAIRE','SURVEILLANT_COLLEGE','SURVEILLANT_LYCEE','SURVEILLANT_GENERAL')
  AND p.code IN ('ELEVE_CONSULTER','CLASSE_CONSULTER','EMPLOI_DU_TEMPS_CONSULTER',
                 'ASSIDUITE_ELEVE_CONSULTER','ASSIDUITE_ELEVE_SAISIR',
                 'ASSIDUITE_ENSEIGNANT_CONSULTER','ASSIDUITE_ENSEIGNANT_SAISIR')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
