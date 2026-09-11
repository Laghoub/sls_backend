package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.exception.FinanceBusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ChargeCalculationService {
    public BigDecimal finalAmount(BigDecimal originalAmount, BigDecimal discountAmount) {
        if (originalAmount == null || discountAmount == null) {
            throw new FinanceBusinessException("Les montants de la créance sont obligatoires.");
        }
        if (originalAmount.signum() < 0 || discountAmount.signum() < 0) {
            throw new FinanceBusinessException("Les montants ne peuvent pas être négatifs.");
        }
        if (discountAmount.compareTo(originalAmount) > 0) {
            throw new FinanceBusinessException("La remise ne peut pas dépasser le montant initial.");
        }
        return originalAmount.subtract(discountAmount);
    }
}
