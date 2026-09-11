INSERT INTO permission (code, name, module, description, active)
VALUES
    (
        'ANNEE_SCOLAIRE_CONSULTER',
        'Consulter les années scolaires',
        'SCHOOL',
        'Permet de consulter les années scolaires.',
        TRUE
    ),
    (
        'ANNEE_SCOLAIRE_CREER',
        'Créer une année scolaire',
        'SCHOOL',
        'Permet de créer une nouvelle année scolaire.',
        TRUE
    ),
    (
        'ANNEE_SCOLAIRE_MODIFIER',
        'Modifier une année scolaire',
        'SCHOOL',
        'Permet de modifier une année scolaire existante.',
        TRUE
    ),
    (
        'ANNEE_SCOLAIRE_DEFINIR_COURANTE',
        'Définir l''année scolaire courante',
        'SCHOOL',
        'Permet de définir l''année scolaire utilisée comme année courante.',
        TRUE
    )
    ON CONFLICT (code) DO NOTHING;


INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         CROSS JOIN permission p
WHERE r.code = 'ADMIN'
  AND p.code IN (
                 'ANNEE_SCOLAIRE_CONSULTER',
                 'ANNEE_SCOLAIRE_CREER',
                 'ANNEE_SCOLAIRE_MODIFIER',
                 'ANNEE_SCOLAIRE_DEFINIR_COURANTE'
    )
    ON CONFLICT DO NOTHING;