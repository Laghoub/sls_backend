-- Permet d'accorder une réduction avant la scolarisation définitive, sur un dossier d'inscription.
ALTER TABLE student_discount ALTER COLUMN student_enrollment_id DROP NOT NULL;
ALTER TABLE student_discount ADD COLUMN IF NOT EXISTS registration_case_id BIGINT;
ALTER TABLE student_discount ADD CONSTRAINT fk_student_discount_registration_case FOREIGN KEY (registration_case_id) REFERENCES registration_case(id);
ALTER TABLE student_discount ADD CONSTRAINT ck_student_discount_target CHECK ((student_enrollment_id IS NOT NULL AND registration_case_id IS NULL) OR (student_enrollment_id IS NULL AND registration_case_id IS NOT NULL));
CREATE INDEX IF NOT EXISTS idx_student_discount_registration_case ON student_discount(registration_case_id);
