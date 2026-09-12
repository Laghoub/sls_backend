package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.StudentCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.util.List;
import java.time.LocalDate;

public interface StudentChargeRepository extends JpaRepository<StudentCharge, Long> {
    Page<StudentCharge> findByRegistrationCaseId(Long id, Pageable p);

    Page<StudentCharge> findByStudentEnrollmentId(Long id, Pageable p);

    List<StudentCharge> findByRegistrationCaseIdAndStatusNot(Long id, String status);

    boolean existsByRegistrationCaseIdAndFeeTypeIdAndStatusNot(
            Long registrationCaseId,
            Long feeTypeId,
            String status
    );

    List<StudentCharge> findByStudentEnrollmentIdOrderByDueDateAscIdAsc(Long id);

    List<StudentCharge> findByRegistrationCaseIdOrderByDueDateAscIdAsc(Long id);

    List<StudentCharge> findByStatusInAndDueDateBeforeOrderByDueDateAsc(
            List<String> statuses, LocalDate date
    );

    boolean existsByStudentEnrollmentIdAndFeeTypeIdAndBillingPeriodStartAndStatusNot(
            Long studentEnrollmentId,
            Long feeTypeId,
            LocalDate billingPeriodStart,
            String status
    );
    boolean existsByStudentEnrollmentIdAndTariffIdAndBillingPeriodStartAndStatusNot(
            Long studentEnrollmentId, Long tariffId, LocalDate billingPeriodStart, String status
    );
}

