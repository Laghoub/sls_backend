INSERT INTO permission (code, name, module, description, active) VALUES
('ELEVE_CONSULTER', 'Consulter les élèves', 'STUDENT', 'Consulter les profils élèves', TRUE),
('ELEVE_CREER', 'Créer un élève', 'STUDENT', 'Créer un profil élève', TRUE),
('ELEVE_MODIFIER', 'Modifier un élève', 'STUDENT', 'Modifier un profil élève', TRUE),
('RESPONSABLE_CONSULTER', 'Consulter les responsables', 'STUDENT', 'Consulter les responsables légaux et familiaux', TRUE),
('RESPONSABLE_CREER', 'Créer un responsable', 'STUDENT', 'Créer un profil responsable', TRUE),
('RESPONSABLE_MODIFIER', 'Modifier un responsable', 'STUDENT', 'Modifier un profil responsable', TRUE),
('LIEN_RESPONSABLE_CONSULTER', 'Consulter les liens élève-responsable', 'STUDENT', 'Consulter les relations entre élèves et responsables', TRUE),
('LIEN_RESPONSABLE_GERER', 'Gérer les liens élève-responsable', 'STUDENT', 'Créer et modifier les relations entre élèves et responsables', TRUE),
('INFO_FAMILIALE_CONSULTER', 'Consulter les informations familiales', 'STUDENT', 'Consulter les informations familiales complémentaires', TRUE),
('INFO_FAMILIALE_GERER', 'Gérer les informations familiales', 'STUDENT', 'Créer et modifier les informations familiales', TRUE),
('PERSONNE_AUTORISEE_CONSULTER', 'Consulter les personnes autorisées', 'STUDENT', 'Consulter les personnes autorisées à récupérer un élève', TRUE),
('PERSONNE_AUTORISEE_GERER', 'Gérer les personnes autorisées', 'STUDENT', 'Créer et modifier les personnes autorisées', TRUE),
('INFO_MEDICALE_ELEVE_CONSULTER', 'Consulter les informations médicales élève', 'STUDENT_MEDICAL', 'Consulter les données médicales sensibles des élèves', TRUE),
('INFO_MEDICALE_ELEVE_GERER', 'Gérer les informations médicales élève', 'STUDENT_MEDICAL', 'Créer et modifier les données médicales sensibles des élèves', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
'ELEVE_CONSULTER','ELEVE_CREER','ELEVE_MODIFIER','RESPONSABLE_CONSULTER','RESPONSABLE_CREER','RESPONSABLE_MODIFIER',
'LIEN_RESPONSABLE_CONSULTER','LIEN_RESPONSABLE_GERER','INFO_FAMILIALE_CONSULTER','INFO_FAMILIALE_GERER',
'PERSONNE_AUTORISEE_CONSULTER','PERSONNE_AUTORISEE_GERER','INFO_MEDICALE_ELEVE_CONSULTER','INFO_MEDICALE_ELEVE_GERER')
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
