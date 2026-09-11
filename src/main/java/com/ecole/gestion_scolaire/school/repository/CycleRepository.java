package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CycleRepository
        extends JpaRepository<Cycle, Long> {

    Optional<Cycle> findByCode(String code);

    boolean existsByCode(String code);

    List<Cycle> findByActiveTrueOrderByDisplayOrderAsc();

    List<Cycle> findAllByOrderByDisplayOrderAsc();
}