package com.ecole.gestion_scolaire.finance.dto.billing;

import java.math.BigDecimal;
import java.util.List;

public record BillingPreviewResponse(
        Long tariffId, Long feeTypeId, String feeTypeCode, String feeTypeName, String billingFrequency,
        int studentCount, int chargeCount, int duplicateCount, BigDecimal totalAmount,
        List<BillingPreviewLineResponse> lines
) {}
