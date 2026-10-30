CREATE TABLE term_evaluation_publication (
 id BIGSERIAL PRIMARY KEY,
 campaign_id BIGINT NOT NULL REFERENCES term_evaluation_campaign(id),
 student_id BIGINT NOT NULL REFERENCES student(id),
 version_number INT NOT NULL,
 published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 snapshot JSONB NOT NULL,
 UNIQUE(campaign_id,student_id,version_number)
);
CREATE INDEX idx_term_publication_parent ON term_evaluation_publication(student_id,campaign_id,published_at DESC);
