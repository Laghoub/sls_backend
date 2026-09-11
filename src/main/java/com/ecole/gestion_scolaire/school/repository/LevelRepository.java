package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LevelRepository
        extends JpaRepository<Level, Long> {

    Optional<Level> findByCode(String code);

    boolean existsByCode(String code);

    List<Level> findByCycleIdOrderByDisplayOrderAsc(
            Long cycleId
    );

    List<Level> findByCycleIdAndActiveTrueOrderByDisplayOrderAsc(
            Long cycleId
    );

    List<Level> findByActiveTrueOrderByDisplayOrderAsc();
}