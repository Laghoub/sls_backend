package com.ecole.gestion_scolaire.finance.dto.allocation;

import java.math.BigDecimal;
import java.util.List;

public record AutomaticAllocationResponse(
        Long guardianId,
        Long schoolYearId,
        BigDecimal receivedAmount,
        BigDecimal allocatedAmount,
        BigDecimal unallocatedAmount,
        int affectedCharges,
        List<AutomaticAllocationLineResponse> allocations
) {}
