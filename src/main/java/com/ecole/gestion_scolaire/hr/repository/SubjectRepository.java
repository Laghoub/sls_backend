package com.ecole.gestion_scolaire.hr.repository;

import com.ecole.gestion_scolaire.hr.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Subject> findAllByOrderByDisplayOrderAscNameAsc();
    Optional<Subject> findByCodeIgnoreCase(String code);
    Optional<Subject> findByNameIgnoreCase(String name);
}
