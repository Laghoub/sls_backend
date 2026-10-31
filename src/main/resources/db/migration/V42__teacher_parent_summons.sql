-- Extension additive : convocations initiées par les enseignants.
ALTER TABLE parent_teacher_appointment ADD COLUMN origin VARCHAR(20) NOT NULL DEFAULT 'PARENT';
ALTER TABLE parent_teacher_appointment ADD CONSTRAINT chk_appointment_origin CHECK (origin IN ('PARENT','TEACHER'));
CREATE INDEX idx_appointment_origin_status ON parent_teacher_appointment(origin,status,created_at DESC);
