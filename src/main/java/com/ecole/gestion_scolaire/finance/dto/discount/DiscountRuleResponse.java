package com.ecole.gestion_scolaire.finance.dto.discount;
import java.math.BigDecimal; import java.time.LocalDate;
public record DiscountRuleResponse(Long id,Long schoolYearId,String code,String name,String discountType,BigDecimal value,Long feeTypeId,Integer priority,boolean requiresApproval,boolean active,LocalDate validFrom,LocalDate validUntil,String conditionType,Integer childRank,Long cycleId,Long levelId,Long classGroupId,Long campusId){}
