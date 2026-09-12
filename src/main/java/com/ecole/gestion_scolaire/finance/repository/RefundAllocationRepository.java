package com.ecole.gestion_scolaire.finance.repository;
import com.ecole.gestion_scolaire.finance.entity.RefundAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RefundAllocationRepository extends JpaRepository<RefundAllocation,Long>{
    List<RefundAllocation> findByRefundIdOrderByIdAsc(Long refundId);
    List<RefundAllocation> findByPaymentAllocationId(Long paymentAllocationId);
    List<RefundAllocation> findByStudentChargeId(Long studentChargeId);
    boolean existsByRefundId(Long refundId);
}
