package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository
        extends JpaRepository<TimeSlot, Long> {

    Optional<TimeSlot> findByCode(String code);

    boolean existsByCode(String code);

    List<TimeSlot> findAllByOrderByDisplayOrderAsc();

    List<TimeSlot> findByActiveTrueOrderByDisplayOrderAsc();
}