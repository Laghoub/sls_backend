INSERT INTO permission (
    code,
    name,
    module,
    description,
    active
)
VALUES
    (
        'CALENDRIER_SCOLAIRE_CONSULTER',
        'Consulter le calendrier scolaire',
        'SCHOOL',
        'Permet de consulter les exceptions du calendrier scolaire.',
        TRUE
    ),
    (
        'CALENDRIER_SCOLAIRE_GERER',
        'Gérer le calendrier scolaire',
        'SCHOOL',
        'Permet de créer et modifier les exceptions du calendrier scolaire.',
        TRUE
    )
    ON CONFLICT (code) DO NOTHING;


INSERT INTO role_permission (
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM role r
         CROSS JOIN permission p
WHERE r.code = 'ADMIN'
  AND p.code IN (
                 'CALENDRIER_SCOLAIRE_CONSULTER',
                 'CALENDRIER_SCOLAIRE_GERER'
    )
    ON CONFLICT DO NOTHING;