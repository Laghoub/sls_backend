package com.ecole.gestion_scolaire.hr.repository;

import com.ecole.gestion_scolaire.hr.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {
    List<JobPosition> findAllByOrderByDisplayOrderAscNameAsc();
    List<JobPosition> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    Optional<JobPosition> findByCodeIgnoreCase(String code);
    Optional<JobPosition> findByNameIgnoreCase(String name);
}
