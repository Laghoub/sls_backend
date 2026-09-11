-- Module FINANCE / PAIEMENTS / CAISSE
CREATE SEQUENCE IF NOT EXISTS payment_number_seq START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE CACHE 1;

INSERT INTO permission (code,name,module,description,active)
SELECT v.code,v.name,'FINANCE',v.description,TRUE FROM (VALUES
 ('FINANCE_PARAMETRER','Paramétrer la finance','Gérer les référentiels financiers'),
 ('TARIF_CONSULTER','Consulter les tarifs','Consulter types de frais et tarifs'),
 ('TARIF_GERER','Gérer les tarifs','Créer et modifier les tarifs'),
 ('CREANCE_CONSULTER','Consulter les créances','Consulter les sommes dues'),
 ('CREANCE_GERER','Gérer les créances','Créer et annuler les créances'),
 ('PAIEMENT_CONSULTER','Consulter les paiements','Consulter paiements et ventilations'),
 ('PAIEMENT_CREER','Créer un paiement','Encaisser et ventiler un paiement'),
 ('PAIEMENT_ANNULER','Annuler un paiement','Annuler un paiement non ventilé'),
 ('CAISSE_CONSULTER','Consulter les caisses','Consulter caisses, sessions et mouvements'),
 ('CAISSE_GERER','Gérer les caisses','Créer et modifier les caisses'),
 ('CAISSE_OUVRIR','Ouvrir une caisse','Ouvrir une session de caisse'),
 ('CAISSE_FERMER','Fermer une caisse','Clôturer une session de caisse'),
 ('CAISSE_MOUVEMENT_GERER','Gérer les mouvements de caisse','Créer un mouvement manuel de caisse'),
 ('REMBOURSEMENT_CONSULTER','Consulter les remboursements','Consulter les remboursements'),
 ('REMBOURSEMENT_CREER','Créer un remboursement','Enregistrer et valider un remboursement')
) AS v(code,name,description)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code=v.code);

INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code='ADMIN' AND p.module='FINANCE'
AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
