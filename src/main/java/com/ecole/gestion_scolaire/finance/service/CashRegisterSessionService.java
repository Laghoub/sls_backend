package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.cash.*;
import com.ecole.gestion_scolaire.finance.entity.CashRegisterSession;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.ecole.gestion_scolaire.common.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class CashRegisterSessionService {
    private final CashRegisterSessionRepository repo;
    private final CashRegisterService registers;
    private final CashMovementRepository movements;
    private final CurrentFinanceAccountService current;

    public CashRegisterSessionService(CashRegisterSessionRepository r, CashRegisterService c, CashMovementRepository m, CurrentFinanceAccountService a) {
        repo = r;
        registers = c;
        movements = m;
        current = a;
    }


    @Transactional(readOnly = true)
    public CashRegisterSessionResponse currentForUser() {
        Long uid = current.id();
        return repo.findFirstByCashierIdAndStatusOrderByOpenedAtDesc(uid, "OPEN")
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<CashRegisterSessionResponse> history(Long cashRegisterId, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("openedAt").descending());
        return PageResponse.from(repo.findByCashRegisterId(cashRegisterId, p).map(this::toResponse));
    }

    @Transactional
    public CashRegisterSessionResponse open(CashSessionOpenRequest r) {
        var cr = registers.get(r.cashRegisterId());
        if (!cr.isActive()) throw new FinanceBusinessException("Cette caisse est inactive.");
        if (repo.findFirstByCashRegisterIdAndStatusOrderByOpenedAtDesc(cr.getId(), "OPEN").isPresent())
            throw new FinanceBusinessException("Cette caisse possède déjà une session ouverte.");
        Long uid = current.id();
        if (repo.findFirstByCashierIdAndStatusOrderByOpenedAtDesc(uid, "OPEN").isPresent())
            throw new FinanceBusinessException("Cet utilisateur possède déjà une session de caisse ouverte.");
        var x = new CashRegisterSession();
        x.setCashRegisterId(cr.getId());
        x.setCashierId(uid);
        x.setOpenedAt(OffsetDateTime.now());
        x.setOpeningBalance(r.openingBalance());
        x.setStatus("OPEN");
        return toResponse(repo.save(x));
    }

    @Transactional(readOnly = true)
    public CashRegisterSession getOpen(Long id) {
        var x = repo.findById(id).orElseThrow(() -> new FinanceNotFoundException("Session de caisse introuvable."));
        if (!"OPEN".equals(x.getStatus())) throw new FinanceBusinessException("La session de caisse est fermée.");
        return x;
    }

    @Transactional
    public CashRegisterSessionResponse close(Long id, CashSessionCloseRequest r) {
        var x = getOpen(id);
        BigDecimal net = movements.findByCashRegisterSessionId(id, org.springframework.data.domain.Pageable.unpaged()).getContent().stream().map(m -> "IN".equals(m.getDirection()) ? m.getAmount() : m.getAmount().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = x.getOpeningBalance().add(net);
        x.setExpectedClosingBalance(expected);
        x.setActualClosingBalance(r.actualClosingBalance());
        x.setDifferenceAmount(r.actualClosingBalance().subtract(expected));
        x.setClosedAt(OffsetDateTime.now());
        x.setClosedBy(current.id());
        x.setStatus("CLOSED");
        return toResponse(repo.save(x));
    }

    public CashRegisterSessionResponse toResponse(CashRegisterSession x) {
        return new CashRegisterSessionResponse(x.getId(), x.getCashRegisterId(), x.getCashierId(), x.getOpenedAt(), x.getOpeningBalance(), x.getClosedAt(), x.getExpectedClosingBalance(), x.getActualClosingBalance(), x.getDifferenceAmount(), x.getStatus(), x.getClosedBy());
    }
}
