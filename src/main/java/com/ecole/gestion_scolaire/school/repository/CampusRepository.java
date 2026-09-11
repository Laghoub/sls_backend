package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampusRepository
        extends JpaRepository<Campus, Long> {

    Optional<Campus> findByCode(String code);

    boolean existsByCode(String code);

    List<Campus> findByActiveTrueOrderByNameAsc();

    List<Campus> findAllByOrderByNameAsc();
}