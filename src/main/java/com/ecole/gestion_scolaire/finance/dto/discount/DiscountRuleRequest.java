package com.ecole.gestion_scolaire.finance.dto.discount;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public record DiscountRuleRequest(@NotNull Long schoolYearId,@NotBlank String code,@NotBlank String name,@NotBlank String discountType,@NotNull @DecimalMin("0.00") BigDecimal value,Long feeTypeId,Integer priority,Boolean requiresApproval,Boolean active,LocalDate validFrom,LocalDate validUntil,String conditionType,Integer childRank,Long cycleId,Long levelId,Long classGroupId,Long campusId){}
