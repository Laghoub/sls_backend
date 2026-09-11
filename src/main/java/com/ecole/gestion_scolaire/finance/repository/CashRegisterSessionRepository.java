package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.CashRegisterSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.util.Optional;

public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSession, Long> {
    Optional<CashRegisterSession> findFirstByCashRegisterIdAndStatusOrderByOpenedAtDesc(Long id, String status);

    Optional<CashRegisterSession> findFirstByCashierIdAndStatusOrderByOpenedAtDesc(Long id, String status);

    Page<CashRegisterSession> findByCashRegisterId(Long id, Pageable p);
}
