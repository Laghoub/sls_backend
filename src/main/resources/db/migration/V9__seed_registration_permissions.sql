-- Module INSCRIPTION / PRE-INSCRIPTION
INSERT INTO permission (code, name, module, description, active)
SELECT v.code, v.name, 'INSCRIPTION', v.description, TRUE
FROM (VALUES
 ('INSCRIPTION_CONSULTER','Consulter les inscriptions','Consulter et rechercher les dossiers d''inscription'),
 ('INSCRIPTION_CREER','Créer une pré-inscription','Créer un nouveau dossier d''inscription'),
 ('INSCRIPTION_MODIFIER','Modifier une inscription','Modifier les données principales d''un dossier'),
 ('INSCRIPTION_COMPLETER','Compléter une inscription','Compléter informations, pièces et consentements'),
 ('INSCRIPTION_FINALISER','Finaliser une inscription','Valider le dossier et créer la scolarisation annuelle'),
 ('INSCRIPTION_ANNULER','Annuler une inscription','Annuler un dossier non finalisé'),
 ('INSCRIPTION_PARAMETRER','Paramétrer les inscriptions','Gérer les exigences documentaires'),
 ('INSCRIPTION_PAIEMENT_CONFIRMER','Confirmer paiement inscription','Point d''intégration réservé au module Finance'),
 ('SCOLARISATION_CONSULTER','Consulter les scolarisations','Consulter les inscriptions annuelles'),
 ('SCOLARISATION_MODIFIER','Modifier les scolarisations','Modifier la classe et l''historique de scolarisation')
) AS v(code,name,description)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code=v.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code='ADMIN' AND p.code IN ('INSCRIPTION_CONSULTER','INSCRIPTION_CREER','INSCRIPTION_MODIFIER','INSCRIPTION_COMPLETER','INSCRIPTION_FINALISER','INSCRIPTION_ANNULER','INSCRIPTION_PARAMETRER','INSCRIPTION_PAIEMENT_CONFIRMER','SCOLARISATION_CONSULTER','SCOLARISATION_MODIFIER')
AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
