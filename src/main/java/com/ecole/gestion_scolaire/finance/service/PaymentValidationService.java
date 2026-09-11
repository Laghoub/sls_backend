package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.payment.PaymentCreateRequest;
import com.ecole.gestion_scolaire.finance.exception.FinanceBusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;

@Service
public class PaymentValidationService {
    private final StudentChargeService charges;

    public PaymentValidationService(StudentChargeService charges) {
        this.charges = charges;
    }

    public BigDecimal validateAndTotalAllocations(PaymentCreateRequest request) {
        if (request.allocations() == null) {
            throw new FinanceBusinessException("La liste des ventilations est obligatoire.");
        }
        var ids = new HashSet<Long>();
        var total = BigDecimal.ZERO;
        for (var allocation : request.allocations()) {
            if (!ids.add(allocation.studentChargeId())) {
                throw new FinanceBusinessException("Une créance ne peut apparaître qu'une fois dans un paiement.");
            }
            var charge = charges.get(allocation.studentChargeId());
            if ("CANCELLED".equals(charge.getStatus()) || "PAID".equals(charge.getStatus())) {
                throw new FinanceBusinessException("Créance non payable : " + charge.getId());
            }
            var remaining = charges.toResponse(charge).remainingAmount();
            if (allocation.amount().compareTo(remaining) > 0) {
                throw new FinanceBusinessException("Le montant dépasse le reste dû pour la créance " + charge.getId());
            }
            total = total.add(allocation.amount());
        }
        if (total.compareTo(request.totalAmount()) > 0) {
            throw new FinanceBusinessException("La ventilation dépasse le montant du paiement.");
        }
        return total;
    }
}
