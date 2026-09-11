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
    Page<Student> search(
            @Param("search") String search,
            Pageable pageable
    );
}
