package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.installment.*;
import com.ecole.gestion_scolaire.finance.entity.*;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.repository.StudentEnrollmentRepository;
import com.ecole.gestion_scolaire.school.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class BillingScheduleService {
    private final StudentEnrollmentRepository enrollments;
    private final SchoolYearRepository years;
    private final ClassGroupRepository classes;
    private final TariffRepository tariffs;
    private final FeeTypeRepository fees;
    private final StudentChargeRepository charges;
    private final DiscountCalculationService discounts;

    public BillingScheduleService(StudentEnrollmentRepository e, SchoolYearRepository y, ClassGroupRepository c,
                                  TariffRepository t, FeeTypeRepository f, StudentChargeRepository sc, DiscountCalculationService discounts) {
        enrollments=e; years=y; classes=c; tariffs=t; fees=f; charges=sc; this.discounts=discounts;
    }

    /**
     * Génération standard utilisée automatiquement à la création d'une
     * scolarisation. Sans type de frais forcé ni période forcée, le moteur
     * sélectionne uniquement les frais actifs de catégorie SCOLARITE et couvre
     * la période allant de la date d'entrée de l'élève à la fin de l'année.
     */
    @Transactional
    public InstallmentGenerationResponse generateDefault(Long enrollmentId) {
        return generate(
                enrollmentId,
                new InstallmentGenerationRequest(null, null, null)
        );
    }

    @Transactional
    public InstallmentGenerationResponse generate(Long enrollmentId, InstallmentGenerationRequest request) {
        var enrollment = enrollments.findById(enrollmentId)
                .orElseThrow(() -> new FinanceNotFoundException("Scolarisation introuvable : " + enrollmentId));
        var year = years.findById(enrollment.getSchoolYearId())
                .orElseThrow(() -> new FinanceNotFoundException("Année scolaire introuvable."));
        var cg = classes.findById(enrollment.getCurrentClassGroupId())
                .orElseThrow(() -> new FinanceNotFoundException("Classe introuvable."));

        LocalDate from = max(enrollment.getEnrollmentDate(), year.getStartDate(), request.fromDate());
        LocalDate to = min(year.getEndDate(), request.toDate());
        if (from.isAfter(to)) throw new FinanceBusinessException("La période de génération est invalide.");

        var allTariffs = tariffs.findBySchoolYearIdAndActiveTrue(year.getId());
        var byFee = new LinkedHashMap<Long, List<Tariff>>();
        for (var t : allTariffs) {
            if (request.feeTypeId()!=null && !request.feeTypeId().equals(t.getFeeTypeId())) continue;
            var fee = fees.findById(t.getFeeTypeId()).orElse(null);
            if (fee==null || !fee.isActive()) continue;
            if (request.feeTypeId()==null && !"SCOLARITE".equalsIgnoreCase(fee.getCategory())) continue;
            if (!scopeMatches(t, cg.getId(), cg.getLevel().getId(), cg.getLevel().getCycle().getId(), cg.getCampus().getId())) continue;
            byFee.computeIfAbsent(t.getFeeTypeId(), k -> new ArrayList<>()).add(t);
        }

        int created=0, skipped=0;
        var ids = new ArrayList<Long>();
        for (var entry : byFee.entrySet()) {
            var fee = fees.findById(entry.getKey()).orElseThrow();
            String frequency = bestFrequency(entry.getValue(), from);
            if (frequency == null) continue;
            for (var period : periods(from, to, frequency)) {
                var tariff = bestTariff(entry.getValue(), period[0]);
                if (tariff == null) { skipped++; continue; }
                if (charges.existsByStudentEnrollmentIdAndFeeTypeIdAndBillingPeriodStartAndStatusNot(
                        enrollmentId, fee.getId(), period[0], "CANCELLED")) { skipped++; continue; }
                var sc = new StudentCharge();
                sc.setStudentEnrollmentId(enrollmentId);
                sc.setFeeTypeId(fee.getId()); sc.setTariffId(tariff.getId());
                sc.setLabel(label(fee.getName(), period[0], frequency));
                var reduction = discounts.calculate(enrollmentId, fee.getId(), tariff.getAmount(), period[0]).discountAmount();
                sc.setOriginalAmount(tariff.getAmount()); sc.setDiscountAmount(reduction);
                sc.setFinalAmount(tariff.getAmount().subtract(reduction).max(java.math.BigDecimal.ZERO)); sc.setDueDate(period[0]);
                sc.setBillingPeriodStart(period[0]); sc.setBillingPeriodEnd(period[1]); sc.setStatus("DUE");
                sc = charges.save(sc); ids.add(sc.getId()); created++;
            }
        }
        return new InstallmentGenerationResponse(enrollmentId, created, skipped, ids);
    }

    private boolean scopeMatches(Tariff t, Long classId, Long levelId, Long cycleId, Long campusId) {
        return (t.getClassGroupId()==null || t.getClassGroupId().equals(classId))
                && (t.getLevelId()==null || t.getLevelId().equals(levelId))
                && (t.getCycleId()==null || t.getCycleId().equals(cycleId))
                && (t.getCampusId()==null || t.getCampusId().equals(campusId));
    }
    private Tariff bestTariff(List<Tariff> list, LocalDate date) {
        return list.stream().filter(t -> !date.isBefore(t.getValidFrom()) && (t.getValidUntil()==null || !date.isAfter(t.getValidUntil())))
                .max(Comparator.comparingInt(this::specificity).thenComparing(Tariff::getValidFrom)).orElse(null);
    }
    private int specificity(Tariff t){ return (t.getClassGroupId()!=null?8:0)+(t.getLevelId()!=null?4:0)+(t.getCycleId()!=null?2:0)+(t.getCampusId()!=null?1:0); }
    private String bestFrequency(List<Tariff> list, LocalDate date){ var t=bestTariff(list,date); return t==null?null:t.getBillingFrequency(); }
    private List<LocalDate[]> periods(LocalDate from, LocalDate to, String f){
        var out=new ArrayList<LocalDate[]>();
        if ("MONTHLY".equalsIgnoreCase(f)) { LocalDate d=from.withDayOfMonth(1); while(!d.isAfter(to)){ var e=d.withDayOfMonth(d.lengthOfMonth()); out.add(new LocalDate[]{max(d,from),min(e,to)}); d=d.plusMonths(1);} }
        else if ("QUARTERLY".equalsIgnoreCase(f)) { LocalDate d=from; while(!d.isAfter(to)){ var e=min(d.plusMonths(3).minusDays(1),to); out.add(new LocalDate[]{d,e}); d=e.plusDays(1);} }
        else out.add(new LocalDate[]{from,to});
        return out;
    }
    private String label(String name, LocalDate start, String f){ return "MONTHLY".equalsIgnoreCase(f) ? name+" - "+start.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)+" "+start.getYear() : name; }
    private LocalDate max(LocalDate... ds){ LocalDate r=null; for(var d:ds) if(d!=null && (r==null||d.isAfter(r))) r=d; return r; }
    private LocalDate min(LocalDate... ds){ LocalDate r=null; for(var d:ds) if(d!=null && (r==null||d.isBefore(r))) r=d; return r; }
}
