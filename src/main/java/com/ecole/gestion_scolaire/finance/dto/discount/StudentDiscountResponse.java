package com.ecole.gestion_scolaire.finance.dto.discount;
import java.math.BigDecimal; import java.time.*;
public record StudentDiscountResponse(Long id,Long studentEnrollmentId,Long feeTypeId,String discountType,BigDecimal value,LocalDate startDate,LocalDate endDate,String reason,String status,Long createdBy,OffsetDateTime createdAt,Long approvedBy,OffsetDateTime approvedAt,Long rejectedBy,OffsetDateTime rejectedAt,String rejectionReason,OffsetDateTime appliedAt){}
