-- Portail enseignant : droits personnels + exercices/evaluations.
INSERT INTO permission(code,name,module,description,active) SELECT v.code,v.name,'PORTAIL_ENSEIGNANT',v.description,TRUE FROM (VALUES
 ('PORTAIL_ENSEIGNANT_ACCEDER','Accéder au portail enseignant','Accéder à son espace enseignant'),
 ('PORTAIL_ENSEIGNANT_REMUNERATION','Consulter ses rémunérations','Consulter uniquement ses propres rémunérations'),
 ('PORTAIL_ENSEIGNANT_ASSIDUITE','Consulter son assiduité','Consulter uniquement ses présences, absences et retards'),
 ('PORTAIL_ENSEIGNANT_CLASSES','Consulter ses classes et élèves','Consulter les classes affectées et leurs élèves'),
 ('PORTAIL_ENSEIGNANT_EMPLOI_DU_TEMPS','Consulter son emploi du temps','Consulter uniquement son propre emploi du temps'),
 ('PORTAIL_ENSEIGNANT_EXERCICES','Gérer ses exercices','Créer et gérer les exercices de ses affectations'),
 ('PORTAIL_ENSEIGNANT_EVALUATIONS','Gérer ses évaluations','Créer les évaluations et saisir les notes de ses élèves')
) v(code,name,description) WHERE NOT EXISTS(SELECT 1 FROM permission p WHERE p.code=v.code);
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='ENSEIGNANT' AND p.module='PORTAIL_ENSEIGNANT'
AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='ADMIN' AND p.module='PORTAIL_ENSEIGNANT'
AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

CREATE TABLE IF NOT EXISTS teacher_exercise (
 id BIGSERIAL PRIMARY KEY, teacher_id BIGINT NOT NULL REFERENCES teacher(id), teaching_assignment_id BIGINT NOT NULL REFERENCES teaching_assignment(id),
 title VARCHAR(200) NOT NULL, description TEXT, instructions TEXT, due_date DATE, attachment_reference VARCHAR(500), status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_teacher_exercise_teacher ON teacher_exercise(teacher_id);

CREATE TABLE IF NOT EXISTS teacher_evaluation (
 id BIGSERIAL PRIMARY KEY, teacher_id BIGINT NOT NULL REFERENCES teacher(id), teaching_assignment_id BIGINT NOT NULL REFERENCES teaching_assignment(id),
 title VARCHAR(200) NOT NULL, evaluation_date DATE NOT NULL, max_score NUMERIC(8,2) NOT NULL, coefficient NUMERIC(8,2) NOT NULL DEFAULT 1,
 description TEXT, status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_teacher_evaluation_teacher ON teacher_evaluation(teacher_id);

CREATE TABLE IF NOT EXISTS teacher_evaluation_grade (
 id BIGSERIAL PRIMARY KEY, evaluation_id BIGINT NOT NULL REFERENCES teacher_evaluation(id) ON DELETE CASCADE, student_id BIGINT NOT NULL REFERENCES student(id),
 score NUMERIC(8,2), absent BOOLEAN NOT NULL DEFAULT FALSE, comment TEXT, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_teacher_evaluation_grade UNIQUE(evaluation_id,student_id)
);
