package com.ecole.gestion_scolaire.finance.repository;
import com.ecole.gestion_scolaire.finance.entity.RefundCreditReversal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RefundCreditReversalRepository extends JpaRepository<RefundCreditReversal,Long>{
    List<RefundCreditReversal> findByRefundIdOrderByIdAsc(Long refundId);
    boolean existsByRefundId(Long refundId);
}
