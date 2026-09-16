-- V19 - Module Assiduité des élèves. Les tables existent depuis V1.
CREATE INDEX IF NOT EXISTS idx_student_attendance_recorded_at ON student_attendance(recorded_at);
CREATE INDEX IF NOT EXISTS idx_student_attendance_student_status ON student_attendance(student_id, attendance_status);
INSERT INTO permission(code,name,module,description,active)
SELECT v.code,v.name,'ASSIDUITE_ELEVE',v.description,TRUE FROM (VALUES
 ('ASSIDUITE_ELEVE_CONSULTER','Consulter l’assiduité des élèves','Consulter les présences, absences et retards des élèves'),
 ('ASSIDUITE_ELEVE_SAISIR','Saisir l’assiduité des élèves','Enregistrer et corriger l’assiduité des élèves')
) v(code,name,description) WHERE NOT EXISTS(SELECT 1 FROM permission p WHERE p.code=v.code);
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code IN ('ADMIN','DIRECTEUR_PEDAGOGIQUE','SURVEILLANT_GENERAL','SURVEILLANT_PRIMAIRE','SURVEILLANT_COLLEGE','SURVEILLANT_LYCEE') AND p.code IN ('ASSIDUITE_ELEVE_CONSULTER','ASSIDUITE_ELEVE_SAISIR') AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='ENSEIGNANT' AND p.code='ASSIDUITE_ELEVE_CONSULTER' AND NOT EXISTS(SELECT 1 FROM role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
