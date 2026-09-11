package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    Page<CashMovement> findByCashRegisterSessionId(Long id, Pageable p);
}
