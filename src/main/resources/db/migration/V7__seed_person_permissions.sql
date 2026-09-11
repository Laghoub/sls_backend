INSERT INTO permission (
    code,
    name,
    module,
    description,
    active
)
VALUES
    (
        'PERSONNE_CONSULTER',
        'Consulter les personnes',
        'IDENTITY',
        'Permet de rechercher et consulter les identités physiques.',
        TRUE
    ),
    (
        'PERSONNE_CREER',
        'Créer une personne',
        'IDENTITY',
        'Permet de créer une identité physique.',
        TRUE
    ),
    (
        'PERSONNE_MODIFIER',
        'Modifier une personne',
        'IDENTITY',
        'Permet de modifier une identité physique.',
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
         JOIN permission p
              ON p.code IN (
                            'PERSONNE_CONSULTER',
                            'PERSONNE_CREER',
                            'PERSONNE_MODIFIER'
                  )
WHERE r.code = 'ADMIN'
    ON CONFLICT DO NOTHING;