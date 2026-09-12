package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.billing.*;
import com.ecole.gestion_scolaire.finance.entity.StudentCharge;
import com.ecole.gestion_scolaire.finance.entity.Tariff;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.entity.StudentEnrollment;
import com.ecole.gestion_scolaire.registration.enums.EnrollmentStatus;
import com.ecole.gestion_scolaire.registration.repository.StudentEnrollmentRepository;
import com.ecole.gestion_scolaire.school.entity.ClassGroup;
import com.ecole.gestion_scolaire.school.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class TariffBillingService {
    private final TariffRepository tariffs;
    private final FeeTypeRepository fees;
    private final StudentChargeRepository charges;
    private final StudentEnrollmentRepository enrollments;
    private final SchoolYearRepository years;
    private final ClassGroupRepository classes;
    private final DiscountCalculationService discounts;

    public TariffBillingService(TariffRepository tariffs, FeeTypeRepository fees,
                                StudentChargeRepository charges, StudentEnrollmentRepository enrollments,
                                SchoolYearRepository years, ClassGroupRepository classes, DiscountCalculationService discounts) {
        this.tariffs = tariffs;
        this.fees = fees;
        this.charges = charges;
        this.enrollments = enrollments;
        this.years = years;
        this.classes = classes;
        this.discounts = discounts;
    }

    /** Prévisualise une facturation sans créer de créance. */
    @Transactional(readOnly = true)
    public BillingPreviewResponse preview(TariffBillingRequest request) {
        Tariff tariff = tariff(request.tariffId());
        var fee = fees.findById(tariff.getFeeTypeId())
                .orElseThrow(() -> new FinanceNotFoundException("Type de frais introuvable."));
        if (!fee.isActive()) throw new FinanceBusinessException("Le type de frais sélectionné est inactif.");

        List<BillingPreviewLineResponse> lines = buildLines(tariff, request);
        int duplicates = (int) lines.stream().filter(BillingPreviewLineResponse::alreadyExists).count();
        int toCreate = lines.size() - duplicates;
        BigDecimal total = lines.stream().filter(l -> !l.alreadyExists())
                .map(BillingPreviewLineResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int students = (int) lines.stream().map(BillingPreviewLineResponse::studentId).distinct().count();

        return new BillingPreviewResponse(tariff.getId(), fee.getId(), fee.getCode(), fee.getName(),
                tariff.getBillingFrequency(), students, toCreate, duplicates, total, List.copyOf(lines));
    }

    /** Crée les créances exactement selon la même règle que la prévisualisation. */
    @Transactional
    public BillingCreationResponse create(TariffBillingRequest request) {
        Tariff tariff = tariff(request.tariffId());
        var fee = fees.findById(tariff.getFeeTypeId())
                .orElseThrow(() -> new FinanceNotFoundException("Type de frais introuvable."));
        List<BillingPreviewLineResponse> lines = buildLines(tariff, request);
        List<Long> ids = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int skipped = 0;

        for (var line : lines) {
            if (line.alreadyExists() || charges.existsByStudentEnrollmentIdAndTariffIdAndBillingPeriodStartAndStatusNot(
                    line.enrollmentId(), tariff.getId(), line.periodStart(), "CANCELLED")) {
                skipped++;
                continue;
            }
            var charge = new StudentCharge();
            charge.setStudentEnrollmentId(line.enrollmentId());
            charge.setFeeTypeId(fee.getId());
            charge.setTariffId(tariff.getId());
            charge.setLabel(label(fee.getName(), line.periodStart(), tariff.getBillingFrequency()));
            BigDecimal reduction = discounts.calculate(line.enrollmentId(), fee.getId(), line.amount(), line.periodStart()).discountAmount();
            charge.setOriginalAmount(line.amount());
            charge.setDiscountAmount(reduction);
            charge.setFinalAmount(line.amount().subtract(reduction).max(BigDecimal.ZERO));
            charge.setDueDate(line.dueDate());
            charge.setBillingPeriodStart(line.periodStart());
            charge.setBillingPeriodEnd(line.periodEnd());
            charge.setStatus("DUE");
            charge = charges.save(charge);
            ids.add(charge.getId());
            total = total.add(line.amount());
        }
        return new BillingCreationResponse(tariff.getId(), ids.size(), skipped, total, List.copyOf(ids));
    }

    private List<BillingPreviewLineResponse> buildLines(Tariff tariff, TariffBillingRequest request) {
        var year = years.findById(tariff.getSchoolYearId())
                .orElseThrow(() -> new FinanceNotFoundException("Année scolaire introuvable."));
        LocalDate from = max(year.getStartDate(), tariff.getValidFrom());
        LocalDate to = min(year.getEndDate(), tariff.getValidUntil());
        if (from == null || to == null || from.isAfter(to))
            throw new FinanceBusinessException("La période du tarif est invalide pour l'année scolaire.");

        List<StudentEnrollment> selected = selectEnrollments(tariff, request);
        List<BillingPreviewLineResponse> out = new ArrayList<>();

        for (StudentEnrollment enrollment : selected) {
            ClassGroup cg = classes.findById(enrollment.getCurrentClassGroupId())
                    .orElseThrow(() -> new FinanceNotFoundException("Classe introuvable : " + enrollment.getCurrentClassGroupId()));
            if (!tariffMatches(tariff, cg)) continue;

            LocalDate studentFrom = max(from, enrollment.getEnrollmentDate());
            if (studentFrom.isAfter(to)) continue;
            for (LocalDate[] period : periods(studentFrom, to, tariff.getBillingFrequency())) {
                LocalDate due = request.dueDate() != null && isOneTime(tariff.getBillingFrequency())
                        ? request.dueDate() : period[0];
                boolean exists = charges.existsByStudentEnrollmentIdAndTariffIdAndBillingPeriodStartAndStatusNot(
                        enrollment.getId(), tariff.getId(), period[0], "CANCELLED");
                var student = enrollment.getStudent();
                var person = student.getPerson();
                out.add(new BillingPreviewLineResponse(
                        enrollment.getId(), student.getId(), student.getStudentNumber(), person.getLastName(), person.getFirstName(),
                        cg.getId(), cg.getName(), cg.getLevel().getId(), cg.getLevel().getName(),
                        cg.getLevel().getCycle().getId(), cg.getLevel().getCycle().getName(),
                        period[0], period[1], due, tariff.getAmount(), exists
                ));
            }
        }
        out.sort(Comparator.comparing(BillingPreviewLineResponse::periodStart)
                .thenComparing(BillingPreviewLineResponse::studentId)
                .thenComparing(BillingPreviewLineResponse::enrollmentId));
        return out;
    }

    private List<StudentEnrollment> selectEnrollments(Tariff tariff, TariffBillingRequest request) {
        List<StudentEnrollment> all = enrollments.findBySchoolYearIdOrderByIdAsc(tariff.getSchoolYearId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE).toList();
        Set<Long> selectedIds = request.studentIds() == null ? Set.of() : new HashSet<>(request.studentIds());

        return all.stream().filter(e -> {
            ClassGroup cg = classes.findById(e.getCurrentClassGroupId()).orElse(null);
            if (cg == null) return false;
            return switch (request.targetType()) {
                case STUDENT -> requireTarget(request.targetId(), "L'élève cible est obligatoire.").equals(e.getStudent().getId());
                case SELECTED_STUDENTS -> {
                    if (selectedIds.isEmpty()) throw new FinanceBusinessException("Sélectionnez au moins un élève.");
                    yield selectedIds.contains(e.getStudent().getId());
                }
                case CLASS_GROUP -> requireTarget(request.targetId(), "La classe cible est obligatoire.").equals(cg.getId());
                case LEVEL -> requireTarget(request.targetId(), "Le niveau cible est obligatoire.").equals(cg.getLevel().getId());
                case CYCLE -> requireTarget(request.targetId(), "Le cycle cible est obligatoire.").equals(cg.getLevel().getCycle().getId());
            };
        }).toList();
    }

    private Long requireTarget(Long id, String message) {
        if (id == null) throw new FinanceBusinessException(message);
        return id;
    }

    private Tariff tariff(Long id) {
        Tariff t = tariffs.findById(id).orElseThrow(() -> new FinanceNotFoundException("Tarif introuvable : " + id));
        if (!t.isActive()) throw new FinanceBusinessException("Le tarif sélectionné est inactif.");
        if (t.getAmount() == null || t.getAmount().signum() < 0) throw new FinanceBusinessException("Montant de tarif invalide.");
        return t;
    }

    private boolean tariffMatches(Tariff t, ClassGroup cg) {
        return (t.getCycleId() == null || t.getCycleId().equals(cg.getLevel().getCycle().getId()))
                && (t.getLevelId() == null || t.getLevelId().equals(cg.getLevel().getId()))
                && (t.getClassGroupId() == null || t.getClassGroupId().equals(cg.getId()))
                && (t.getCampusId() == null || t.getCampusId().equals(cg.getCampus().getId()));
    }

    private List<LocalDate[]> periods(LocalDate from, LocalDate to, String frequency) {
        List<LocalDate[]> out = new ArrayList<>();
        String f = frequency == null ? "ONE_TIME" : frequency.toUpperCase(Locale.ROOT);
        if ("MONTHLY".equals(f)) {
            LocalDate d = from.withDayOfMonth(1);
            while (!d.isAfter(to)) {
                LocalDate end = d.withDayOfMonth(d.lengthOfMonth());
                out.add(new LocalDate[]{max(d, from), min(end, to)});
                d = d.plusMonths(1);
            }
        } else if ("QUARTERLY".equals(f)) {
            LocalDate d = from;
            while (!d.isAfter(to)) {
                LocalDate end = min(d.plusMonths(3).minusDays(1), to);
                out.add(new LocalDate[]{d, end});
                d = end.plusDays(1);
            }
        } else {
            out.add(new LocalDate[]{from, to});
        }
        return out;
    }

    private boolean isOneTime(String f) {
        return f == null || "ONE_TIME".equalsIgnoreCase(f) || "ANNUAL".equalsIgnoreCase(f);
    }

    private String label(String name, LocalDate start, String frequency) {
        if ("MONTHLY".equalsIgnoreCase(frequency))
            return name + " - " + start.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + start.getYear();
        return name;
    }

    private LocalDate max(LocalDate... dates) {
        LocalDate result = null;
        for (LocalDate d : dates) if (d != null && (result == null || d.isAfter(result))) result = d;
        return result;
    }

    private LocalDate min(LocalDate... dates) {
        LocalDate result = null;
        for (LocalDate d : dates) if (d != null && (result == null || d.isBefore(result))) result = d;
        return result;
    }
}
