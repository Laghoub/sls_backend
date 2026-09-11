package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Page<Refund> findByPaymentId(Long id, Pageable p);
}
