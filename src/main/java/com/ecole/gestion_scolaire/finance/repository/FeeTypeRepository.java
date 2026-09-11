package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.util.Optional;

public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {
    Optional<FeeType> findByCodeIgnoreCase(String code);

    Page<FeeType> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable p);
}
