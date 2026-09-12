package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.FamilyCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.util.List;

public interface FamilyCreditRepository extends JpaRepository<FamilyCredit, Long> {
    List<FamilyCredit> findByGuardianIdAndStatusNotOrderByCreatedAtAsc(Long id, String status);

    Page<FamilyCredit> findByGuardianId(Long id, Pageable p);

    List<FamilyCredit> findBySourcePaymentIdOrderByCreatedAtAsc(Long paymentId);
}
