package com.ecole.gestion_scolaire.student.repository;

import com.ecole.gestion_scolaire.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByPersonId(Long personId);

    Optional<Student> findByStudentNumber(String studentNumber);

    boolean existsByPersonId(Long personId);

    boolean existsByStudentNumber(String studentNumber);

    List<Student> findAllByOrderByStudentNumberAsc();

    @Query("""
        SELECT s
        FROM Student s
        JOIN s.person p
        WHERE
            :search IS NULL
            OR :search = ''
            OR LOWER(p.lastName)
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.firstName)
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(CONCAT(p.firstName, ' ', p.lastName))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(CONCAT(p.lastName, ' ', p.firstName))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.studentNumber)
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.status)
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(p.phone, ''))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(p.email, ''))
                LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<Student> search(@Param("search") String search, Pageable pageable);

    @Query("""
      SELECT DISTINCT s FROM Student s JOIN s.person p
      WHERE EXISTS (SELECT e.id FROM StudentEnrollment e, ClassGroup cg
                    WHERE e.student = s AND cg.id = e.currentClassGroupId
                    AND cg.level.cycle.code = :cycle AND e.status = com.ecole.gestion_scolaire.registration.enums.EnrollmentStatus.ACTIVE)
      AND (:search IS NULL OR :search = '' OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :search, '%')))
      """)
    Page<Student> searchForCycle(@Param("search") String search, @Param("cycle") String cycle, Pageable pageable);

    @Query("""
      SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM StudentEnrollment e, ClassGroup cg
      WHERE e.student.id = :studentId AND cg.id = e.currentClassGroupId
      AND cg.level.cycle.code = :cycle AND e.status = com.ecole.gestion_scolaire.registration.enums.EnrollmentStatus.ACTIVE
      """)
    boolean isStudentInCycle(@Param("studentId") Long studentId, @Param("cycle") String cycle);


}
