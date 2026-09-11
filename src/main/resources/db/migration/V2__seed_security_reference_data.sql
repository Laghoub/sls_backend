-- ============================================================
-- V2 - Initialisation des rôles et permissions de sécurité
-- ============================================================

-- ------------------------------------------------------------
-- RÔLES SYSTÈME
-- ------------------------------------------------------------

INSERT INTO role (
    code,
    name,
    description,
    system_role,
    active,
    created_at
)
VALUES
    ('ADMIN', 'Administrateur', 'Accès complet à l''administration du système.', TRUE, TRUE, CURRENT_TIMESTAMP),

    ('DIRECTEUR_PEDAGOGIQUE', 'Directeur pédagogique',
     'Gestion pédagogique, enseignants, classes, notes et bulletins.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('DIRECTEUR_FINANCIER', 'Directeur financier',
     'Supervision financière, paie, tarifs, dépenses et validation.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('SURVEILLANT_GENERAL', 'Surveillant général',
     'Supervision générale de l''assiduité et de la vie scolaire.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('SURVEILLANT_PRIMAIRE', 'Surveillant primaire',
     'Vie scolaire et assiduité du cycle primaire.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('SURVEILLANT_COLLEGE', 'Surveillant collège',
     'Vie scolaire et assiduité du cycle collège.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('SURVEILLANT_LYCEE', 'Surveillant lycée',
     'Vie scolaire et assiduité du cycle lycée.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('COMPTABLE', 'Comptable',
     'Encaissements, caisse et opérations financières autorisées.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('ASSISTANTE', 'Assistante',
     'Gestion administrative et finalisation des dossiers.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('ENSEIGNANT', 'Enseignant',
     'Accès enseignant aux classes, notes, emploi du temps et informations autorisées.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('PARENT', 'Parent',
     'Accès parent aux informations de ses enfants.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('ELEVE', 'Élève',
     'Accès élève à ses informations pédagogiques.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('STAFF_ADMINISTRATIF', 'Personnel administratif',
     'Personnel administratif avec permissions configurables.',
     TRUE, TRUE, CURRENT_TIMESTAMP),

    ('CHAUFFEUR', 'Chauffeur',
     'Gestion des tournées de transport affectées au chauffeur.',
     TRUE, TRUE, CURRENT_TIMESTAMP)

    ON CONFLICT (code) DO NOTHING;


-- ------------------------------------------------------------
-- PERMISSIONS DU SOCLE SÉCURITÉ
-- ------------------------------------------------------------

INSERT INTO permission (
    code,
    name,
    module,
    description,
    active
)
VALUES
    (
        'UTILISATEUR_CONSULTER',
        'Consulter les utilisateurs',
        'SECURITY',
        'Permet de consulter les comptes utilisateurs.',
        TRUE
    ),

    (
        'UTILISATEUR_CREER',
        'Créer un utilisateur',
        'SECURITY',
        'Permet de créer un nouveau compte utilisateur.',
        TRUE
    ),

    (
        'UTILISATEUR_MODIFIER',
        'Modifier un utilisateur',
        'SECURITY',
        'Permet de modifier les informations d''un compte utilisateur.',
        TRUE
    ),

    (
        'UTILISATEUR_ACTIVER',
        'Activer un utilisateur',
        'SECURITY',
        'Permet d''activer un compte utilisateur.',
        TRUE
    ),

    (
        'UTILISATEUR_DESACTIVER',
        'Désactiver un utilisateur',
        'SECURITY',
        'Permet de désactiver un compte utilisateur.',
        TRUE
    ),

    (
        'UTILISATEUR_DEVERROUILLER',
        'Déverrouiller un utilisateur',
        'SECURITY',
        'Permet de déverrouiller un compte utilisateur.',
        TRUE
    ),

    (
        'ROLE_CONSULTER',
        'Consulter les rôles',
        'SECURITY',
        'Permet de consulter les rôles du système.',
        TRUE
    ),

    (
        'ROLE_AFFECTER',
        'Affecter un rôle',
        'SECURITY',
        'Permet d''attribuer un rôle à un utilisateur.',
        TRUE
    ),

    (
        'PERMISSION_CONSULTER',
        'Consulter les permissions',
        'SECURITY',
        'Permet de consulter les permissions disponibles.',
        TRUE
    ),

    (
        'PERMISSION_AFFECTER',
        'Affecter une permission',
        'SECURITY',
        'Permet d''affecter des permissions aux rôles.',
        TRUE
    ),

    (
        'SCOPE_GERER',
        'Gérer les périmètres d''accès',
        'SECURITY',
        'Permet de gérer les scopes de sécurité des utilisateurs.',
        TRUE
    )

    ON CONFLICT (code) DO NOTHING;


-- ------------------------------------------------------------
-- ADMIN reçoit toutes les permissions existantes
-- ------------------------------------------------------------

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
    ON CONFLICT DO NOTHING;