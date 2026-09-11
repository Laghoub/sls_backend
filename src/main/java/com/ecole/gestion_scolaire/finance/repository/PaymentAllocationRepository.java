package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findByPaymentIdOrderByIdAsc(Long id);

    List<PaymentAllocation> findByStudentChargeId(Long id);
}
