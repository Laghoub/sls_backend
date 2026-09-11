package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.tariff.*;
import com.ecole.gestion_scolaire.finance.entity.Tariff;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TariffService {
    private final TariffRepository repo;
    private final FeeTypeRepository fees;
    private final FinanceReferenceValidator refs;

    public TariffService(TariffRepository r, FeeTypeRepository f, FinanceReferenceValidator v) {
        repo = r;
        fees = f;
        refs = v;
    }

    @Transactional(readOnly = true)
    public PageResponse<TariffResponse> search(Long year, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("validFrom").descending());
        var x = year == null ? repo.findAll(p) : repo.findBySchoolYearId(year, p);
        return PageResponse.from(x.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public Tariff get(Long id) {
        return repo.findById(id).orElseThrow(() -> new FinanceNotFoundException("Tarif introuvable : " + id));
    }

    @Transactional
    public TariffResponse create(TariffRequest r) {
        var x = new Tariff();
        apply(x, r);
        return toResponse(repo.save(x));
    }

    @Transactional
    public TariffResponse update(Long id, TariffRequest r) {
        var x = get(id);
        apply(x, r);
        return toResponse(repo.save(x));
    }

    private void apply(Tariff x, TariffRequest r) {
        refs.schoolYear(r.schoolYearId());
        refs.level(r.levelId());
        refs.classGroup(r.classGroupId());
        refs.campus(r.campusId());
        if (!fees.existsById(r.feeTypeId()))
            throw new FinanceNotFoundException("Type de frais introuvable : " + r.feeTypeId());
        if (r.validUntil() != null && r.validUntil().isBefore(r.validFrom()))
            throw new FinanceBusinessException("La date de fin du tarif est antérieure à la date de début.");
        x.setSchoolYearId(r.schoolYearId());
        x.setFeeTypeId(r.feeTypeId());
        x.setLevelId(r.levelId());
        x.setClassGroupId(r.classGroupId());
        x.setCampusId(r.campusId());
        x.setAmount(r.amount());
        x.setBillingFrequency(r.billingFrequency().trim().toUpperCase());
        x.setValidFrom(r.validFrom());
        x.setValidUntil(r.validUntil());
        x.setActive(r.active());
    }

    public TariffResponse toResponse(Tariff x) {
        return new TariffResponse(x.getId(), x.getSchoolYearId(), x.getFeeTypeId(), x.getLevelId(), x.getClassGroupId(), x.getCampusId(), x.getAmount(), x.getBillingFrequency(), x.getValidFrom(), x.getValidUntil(), x.isActive(), x.getCreatedAt(), x.getUpdatedAt());
    }
}
