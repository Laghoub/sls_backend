package com.ecole.gestion_scolaire.student.repository;
import com.ecole.gestion_scolaire.student.entity.StudentMedicalInfo; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface StudentMedicalInfoRepository extends JpaRepository<StudentMedicalInfo,Long>{ Optional<StudentMedicalInfo> findByStudentId(Long studentId); }
