-- V24 - Portails parent/eleve + activation securisee des comptes.
INSERT INTO permission(code,name,module,description,active) SELECT v.code,v.name,v.module,v.description,TRUE FROM (VALUES
 ('PORTAIL_PARENT_ACCEDER','Accéder au portail parent','PORTAIL_PARENT','Accéder à son espace parent'),
 ('PORTAIL_PARENT_ENFANTS','Consulter ses enfants','PORTAIL_PARENT','Consulter uniquement les enfants rattachés au parent'),
 ('PORTAIL_PARENT_PEDAGOGIE','Suivi pédagogique des enfants','PORTAIL_PARENT','Consulter classe, emploi du temps, assiduité, exercices, évaluations et notes'),
 ('PORTAIL_PARENT_FINANCES','Consulter la situation financière','PORTAIL_PARENT','Consulter créances, paiements, reste à payer et reçus de ses enfants'),
 ('PORTAIL_ELEVE_ACCEDER','Accéder au portail élève','PORTAIL_ELEVE','Accéder à son espace élève'),
 ('PORTAIL_ELEVE_PEDAGOGIE','Consulter son suivi pédagogique','PORTAIL_ELEVE','Consulter uniquement ses propres données pédagogiques'),
 ('COMPTE_ACTIVER','Activer les comptes utilisateurs','SECURITE','Activer ou renvoyer une invitation aux comptes parent, enseignant, élève et personnel')
) v(code,name,module,description) WHERE NOT EXISTS(SELECT 1 FROM permission p WHERE p.code=v.code);

INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='PARENT' AND p.module='PORTAIL_PARENT'
AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='ELEVE' AND p.module='PORTAIL_ELEVE'
AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='ADMIN' AND p.code='COMPTE_ACTIVER'
AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

CREATE TABLE IF NOT EXISTS account_activation_token (
 id BIGSERIAL PRIMARY KEY,
 user_account_id BIGINT NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 used_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_account_activation_user ON account_activation_token(user_account_id);
CREATE INDEX IF NOT EXISTS idx_account_activation_expiry ON account_activation_token(expires_at);
