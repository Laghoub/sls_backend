package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.util.List;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    Page<Tariff> findBySchoolYearId(Long schoolYearId, Pageable pageable);

    List<Tariff> findBySchoolYearIdAndFeeTypeIdAndActiveTrue(
            Long schoolYearId,
            Long feeTypeId
    );

    List<Tariff> findBySchoolYearIdAndActiveTrue(Long schoolYearId);
}