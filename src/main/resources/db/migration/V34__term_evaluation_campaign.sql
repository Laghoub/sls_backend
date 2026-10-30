
CREATE TABLE term_evaluation_campaign (
 id BIGSERIAL PRIMARY KEY, school_year_id BIGINT NOT NULL,
 title VARCHAR(180) NOT NULL, trimester SMALLINT NOT NULL CHECK(trimester BETWEEN 1 AND 3),
 cycle_code VARCHAR(4) CHECK(cycle_code IS NULL OR cycle_code IN ('PR','CO','LY')),
 evaluation_from DATE NOT NULL, evaluation_to DATE NOT NULL,
 opens_at TIMESTAMPTZ NOT NULL, closes_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK(evaluation_from<=evaluation_to AND opens_at<closes_at),
 CHECK(status IN ('DRAFT','SCHEDULED','OPEN','CLOSED','REVIEW','PUBLISHED','ARCHIVED'))
);
CREATE TABLE term_evaluation_campaign_class (
 campaign_id BIGINT NOT NULL REFERENCES term_evaluation_campaign(id) ON DELETE CASCADE,
 class_group_id BIGINT NOT NULL, PRIMARY KEY(campaign_id,class_group_id)
);
CREATE TABLE term_evaluation_criterion (
 id BIGSERIAL PRIMARY KEY, code VARCHAR(90) NOT NULL UNIQUE,
 section SMALLINT NOT NULL CHECK(section BETWEEN 1 AND 4),
 label_fr VARCHAR(250) NOT NULL, label_ar VARCHAR(250),
 cycle_code VARCHAR(4), active BOOLEAN NOT NULL DEFAULT TRUE,
 sort_order INT NOT NULL DEFAULT 0
);
CREATE TABLE term_evaluation_option (
 id BIGSERIAL PRIMARY KEY,
 criterion_id BIGINT NOT NULL REFERENCES term_evaluation_criterion(id) ON DELETE CASCADE,
 code VARCHAR(80) NOT NULL, label_fr VARCHAR(200) NOT NULL,
 label_ar VARCHAR(200), sort_order INT NOT NULL DEFAULT 0,
 UNIQUE(criterion_id,code)
);
CREATE TABLE term_student_evaluation (
 id BIGSERIAL PRIMARY KEY,
 campaign_id BIGINT NOT NULL REFERENCES term_evaluation_campaign(id),
 student_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL,
 class_group_id BIGINT NOT NULL, subject_id BIGINT NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
 observation TEXT, difficulties TEXT, recommendations TEXT,
 submitted_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(campaign_id,student_id,teacher_id,class_group_id,subject_id),
 CHECK(status IN ('DRAFT','SUBMITTED','RETURNED','APPROVED'))
);
CREATE TABLE term_evaluation_answer (
 evaluation_id BIGINT NOT NULL REFERENCES term_student_evaluation(id) ON DELETE CASCADE,
 criterion_id BIGINT NOT NULL REFERENCES term_evaluation_criterion(id),
 option_id BIGINT REFERENCES term_evaluation_option(id),
 free_text TEXT, PRIMARY KEY(evaluation_id,criterion_id)
);
CREATE INDEX idx_term_eval_campaign ON term_student_evaluation(campaign_id,status);
CREATE INDEX idx_term_eval_teacher ON term_student_evaluation(teacher_id,campaign_id);
INSERT INTO permission(code,name,module,description,active) VALUES
 ('EVALUATION_TRIMESTRIELLE_CAMPAGNE_GERER','Gérer les campagnes','EVALUATION_TRIMESTRIELLE','Campagnes trimestrielles',TRUE),
 ('EVALUATION_TRIMESTRIELLE_SAISIR','Saisir les évaluations','EVALUATION_TRIMESTRIELLE','Saisie enseignant',TRUE),
 ('EVALUATION_TRIMESTRIELLE_SOUMETTRE','Soumettre les évaluations','EVALUATION_TRIMESTRIELLE','Soumission enseignant',TRUE),
 ('EVALUATION_TRIMESTRIELLE_CONTROLER','Contrôler les évaluations','EVALUATION_TRIMESTRIELLE','Contrôle administratif',TRUE),
 ('EVALUATION_TRIMESTRIELLE_PUBLIER','Publier les évaluations','EVALUATION_TRIMESTRIELLE','Publication parent',TRUE),
 ('EVALUATION_TRIMESTRIELLE_CONSULTER','Consulter les évaluations','EVALUATION_TRIMESTRIELLE','Consultation autorisée',TRUE),
 ('EVALUATION_TRIMESTRIELLE_EXPORTER','Exporter les évaluations','EVALUATION_TRIMESTRIELLE','Export PDF',TRUE)
ON CONFLICT(code) DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code='ADMIN' AND p.code LIKE 'EVALUATION_TRIMESTRIELLE_%'
ON CONFLICT DO NOTHING;
