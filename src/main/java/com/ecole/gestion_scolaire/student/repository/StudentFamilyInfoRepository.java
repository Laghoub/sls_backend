package com.ecole.gestion_scolaire.student.repository;
import com.ecole.gestion_scolaire.student.entity.StudentFamilyInfo; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface StudentFamilyInfoRepository extends JpaRepository<StudentFamilyInfo,Long>{ Optional<StudentFamilyInfo> findByStudentId(Long studentId); }
