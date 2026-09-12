package com.ecole.gestion_scolaire.finance.repository;
import com.ecole.gestion_scolaire.finance.entity.StudentDiscount; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.domain.*; import java.util.*;
public interface StudentDiscountRepository extends JpaRepository<StudentDiscount,Long>{ Page<StudentDiscount> findByStatus(String status,Pageable p); Page<StudentDiscount> findByStudentEnrollmentId(Long id,Pageable p); List<StudentDiscount> findByStudentEnrollmentIdAndStatus(Long id,String status); }
