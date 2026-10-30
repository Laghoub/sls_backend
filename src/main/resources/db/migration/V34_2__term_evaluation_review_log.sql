CREATE TABLE term_evaluation_review_log (
 id BIGSERIAL PRIMARY KEY,
 evaluation_id BIGINT NOT NULL REFERENCES term_student_evaluation(id),
 action VARCHAR(30) NOT NULL,
 reason TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_term_eval_review_log_evaluation ON term_evaluation_review_log(evaluation_id,created_at DESC);
