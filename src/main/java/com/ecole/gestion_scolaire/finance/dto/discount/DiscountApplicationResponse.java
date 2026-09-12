package com.ecole.gestion_scolaire.finance.dto.discount;
import java.math.BigDecimal;
public record DiscountApplicationResponse(int chargesUpdated,BigDecimal totalReductionApplied,BigDecimal familyCreditCreated){}
