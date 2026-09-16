package com.ecole.gestion_scolaire.payroll.repository;
import com.ecole.gestion_scolaire.payroll.entity.PayrollEmailOutbox; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import org.springframework.transaction.annotation.Transactional; import java.time.OffsetDateTime; import java.util.*;
public interface PayrollEmailOutboxRepository extends JpaRepository<PayrollEmailOutbox,Long>{
 List<PayrollEmailOutbox> findTop50ByStatusOrderByCreatedAtAsc(String status);
 boolean existsBySalaryPaymentIdAndRecipientEmailIgnoreCase(Long paymentId,String email);
 @Modifying @Transactional @Query("update PayrollEmailOutbox e set e.status='PROCESSING', e.processingAt=:now, e.attemptCount=e.attemptCount+1 where e.id=:id and e.status in ('PENDING','FAILED')") int claim(@Param("id") Long id,@Param("now") OffsetDateTime now);
 @Modifying @Transactional @Query("update PayrollEmailOutbox e set e.status='PENDING', e.processingAt=null where e.status='PROCESSING' and e.processingAt<:cutoff") int recoverStale(@Param("cutoff") OffsetDateTime cutoff);
}
