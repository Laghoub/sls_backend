INSERT INTO permission (code, name, module, description, active)
VALUES

    -- CYCLES
    (
        'CYCLE_CONSULTER',
        'Consulter les cycles',
        'SCHOOL',
        'Permet de consulter les cycles scolaires.',
        TRUE
    ),
    (
        'CYCLE_CREER',
        'Créer un cycle',
        'SCHOOL',
        'Permet de créer un cycle scolaire.',
        TRUE
    ),
    (
        'CYCLE_MODIFIER',
        'Modifier un cycle',
        'SCHOOL',
        'Permet de modifier un cycle scolaire.',
        TRUE
    ),

    -- NIVEAUX
    (
        'NIVEAU_CONSULTER',
        'Consulter les niveaux',
        'SCHOOL',
        'Permet de consulter les niveaux scolaires.',
        TRUE
    ),
    (
        'NIVEAU_CREER',
        'Créer un niveau',
        'SCHOOL',
        'Permet de créer un niveau scolaire.',
        TRUE
    ),
    (
        'NIVEAU_MODIFIER',
        'Modifier un niveau',
        'SCHOOL',
        'Permet de modifier un niveau scolaire.',
        TRUE
    ),

    -- CAMPUS
    (
        'CAMPUS_CONSULTER',
        'Consulter les campus',
        'SCHOOL',
        'Permet de consulter les campus.',
        TRUE
    ),
    (
        'CAMPUS_CREER',
        'Créer un campus',
        'SCHOOL',
        'Permet de créer un campus.',
        TRUE
    ),
    (
        'CAMPUS_MODIFIER',
        'Modifier un campus',
        'SCHOOL',
        'Permet de modifier un campus.',
        TRUE
    ),

    -- SALLES
    (
        'SALLE_CONSULTER',
        'Consulter les salles',
        'SCHOOL',
        'Permet de consulter les salles.',
        TRUE
    ),
    (
        'SALLE_CREER',
        'Créer une salle',
        'SCHOOL',
        'Permet de créer une salle.',
        TRUE
    ),
    (
        'SALLE_MODIFIER',
        'Modifier une salle',
        'SCHOOL',
        'Permet de modifier une salle.',
        TRUE
    ),

    -- CLASSES
    (
        'CLASSE_CONSULTER',
        'Consulter les classes',
        'SCHOOL',
        'Permet de consulter les classes.',
        TRUE
    ),
    (
        'CLASSE_CREER',
        'Créer une classe',
        'SCHOOL',
        'Permet de créer une classe.',
        TRUE
    ),
    (
        'CLASSE_MODIFIER',
        'Modifier une classe',
        'SCHOOL',
        'Permet de modifier une classe.',
        TRUE
    ),

    -- CRÉNEAUX
    (
        'CRENEAU_CONSULTER',
        'Consulter les créneaux horaires',
        'SCHOOL',
        'Permet de consulter les créneaux horaires.',
        TRUE
    ),
    (
        'CRENEAU_CREER',
        'Créer un créneau horaire',
        'SCHOOL',
        'Permet de créer un créneau horaire.',
        TRUE
    ),
    (
        'CRENEAU_MODIFIER',
        'Modifier un créneau horaire',
        'SCHOOL',
        'Permet de modifier un créneau horaire.',
        TRUE
    )

    ON CONFLICT (code) DO NOTHING;


INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         CROSS JOIN permission p
WHERE r.code = 'ADMIN'
  AND p.module = 'SCHOOL'
    ON CONFLICT DO NOTHING;