package com.ecole.gestion_scolaire.finance.dto.payment;

import com.ecole.gestion_scolaire.finance.dto.credit.FamilyCreditResponse;

import java.util.List;

public record PaymentDetailResponse(PaymentResponse payment, List<PaymentAllocationResponse> allocations,
                                    FamilyCreditResponse generatedCredit) {
}
