-- V33 : nouveaux droits, attribués uniquement à ADMIN par défaut.
-- Les autres profils, y compris le comptable, sont configurables via Gestion des droits.
INSERT INTO permission (code,name,module,description,active) VALUES
 ('POINTAGE_MENSUEL_CONSULTER','Consulter le pointage mensuel','ATTENDANCE','Tableau mensuel des heures des enseignants',TRUE),
 ('POINTAGE_MENSUEL_EXPORTER','Exporter le pointage mensuel','ATTENDANCE','Export CSV du pointage des enseignants',TRUE),
 ('POINTAGE_MENSUEL_VALIDER','Valider le pointage mensuel','ATTENDANCE','Validation du pointage mensuel (réservée à une évolution ultérieure)',TRUE)
ON CONFLICT (code) DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code='ADMIN' AND p.code IN
 ('POINTAGE_MENSUEL_CONSULTER','POINTAGE_MENSUEL_EXPORTER','POINTAGE_MENSUEL_VALIDER')
ON CONFLICT DO NOTHING;
