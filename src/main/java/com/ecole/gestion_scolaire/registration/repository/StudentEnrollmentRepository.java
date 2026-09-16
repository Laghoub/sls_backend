package com.ecole.gestion_scolaire.registration.repository;
import com.ecole.gestion_scolaire.registration.entity.StudentEnrollment;
import com.ecole.gestion_scolaire.registration.enums.EnrollmentStatus;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment,Long>{
 boolean existsByStudentIdAndSchoolYearId(Long studentId,Long yearId); Optional<StudentEnrollment> findByStudentIdAndSchoolYearId(Long studentId,Long yearId); Page<StudentEnrollment> findBySchoolYearId(Long yearId,Pageable pageable); List<StudentEnrollment> findByStudentIdOrderByEnrollmentDateDescIdDesc(Long studentId); List<StudentEnrollment> findBySchoolYearIdOrderByIdAsc(Long schoolYearId);
 @Query("select e from StudentEnrollment e join fetch e.student s join fetch s.person p " +
        "where e.schoolYearId = :schoolYearId and e.currentClassGroupId = :classGroupId " +
        "and e.status = :status order by p.lastName, p.firstName")
 List<StudentEnrollment> findActiveClass(Long schoolYearId,Long classGroupId,EnrollmentStatus status);
}
