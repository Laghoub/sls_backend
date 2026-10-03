package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.global.GlobalFinancialSituationResponse;
import com.ecole.gestion_scolaire.finance.dto.global.GlobalFinancialSituationResponse.*;
import com.ecole.gestion_scolaire.finance.entity.StudentCharge;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.entity.StudentEnrollment;
import com.ecole.gestion_scolaire.registration.repository.*;
import com.ecole.gestion_scolaire.school.repository.ClassGroupRepository;
import com.ecole.gestion_scolaire.student.entity.StudentGuardian;
import com.ecole.gestion_scolaire.student.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GlobalFinancialSituationService {

    private static final List<Integer> SCHOOL_MONTHS =
            List.of(9, 10, 11, 12, 1, 2, 3, 4, 5, 6);

    private final StudentEnrollmentRepository enrollments;
    private final RegistrationCaseRepository registrations;
    private final StudentChargeRepository charges;
    private final StudentGuardianRepository links;
    private final GuardianRepository guardians;
    private final FeeTypeRepository fees;
    private final PaymentAllocationService allocations;
    private final FamilyCreditRepository credits;
    private final ClassGroupRepository classes;

    public GlobalFinancialSituationService(
            StudentEnrollmentRepository enrollments,
            RegistrationCaseRepository registrations,
            StudentChargeRepository charges,
            StudentGuardianRepository links,
            GuardianRepository guardians,
            FeeTypeRepository fees,
            PaymentAllocationService allocations,
            FamilyCreditRepository credits,
            ClassGroupRepository classes
    ) {
        this.enrollments = enrollments;
        this.registrations = registrations;
        this.charges = charges;
        this.links = links;
        this.guardians = guardians;
        this.fees = fees;
        this.allocations = allocations;
        this.credits = credits;
        this.classes = classes;
    }

    @Transactional(readOnly = true)
    public GlobalFinancialSituationResponse get(
            Long schoolYearId,
            Long classGroupId,
            String parent,
            Integer cutoffMonth,
            String status
    ) {

        int cut = cutoffMonth == null
                ? currentSchoolMonth()
                : normalizeCutoff(cutoffMonth);

        String q = parent == null
                ? ""
                : parent.trim().toLowerCase(Locale.ROOT);

        String wantedStatus = status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);

        List<StudentEnrollment> ens =
                enrollments.findBySchoolYearIdOrderByIdAsc(schoolYearId)
                        .stream()
                        .filter(e ->
                                classGroupId == null
                                        || Objects.equals(
                                        e.getCurrentClassGroupId(),
                                        classGroupId
                                )
                        )
                        .toList();

        Map<Long, String> classNames =
                classes.findBySchoolYearIdOrderByNameAsc(schoolYearId)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        x -> x.getId(),
                                        x -> x.getName(),
                                        (a, b) -> a
                                )
                        );

        Map<Long, List<StudentEnrollment>> byGuardian =
                new LinkedHashMap<>();

        for (var e : ens) {

            var active =
                    links.findByStudentIdOrderByIdAsc(
                                    e.getStudent().getId()
                            )
                            .stream()
                            .filter(StudentGuardian::isActive)
                            .toList();

            var financial =
                    active.stream()
                            .filter(StudentGuardian::isFinancialResponsible)
                            .toList();

            var selected =
                    financial.isEmpty()
                            ? active.stream()
                            .filter(StudentGuardian::isPrimaryContact)
                            .toList()
                            : financial;

            if (selected.isEmpty() && !active.isEmpty()) {
                selected = List.of(active.get(0));
            }

            for (var l : selected) {
                byGuardian
                        .computeIfAbsent(
                                l.getGuardian().getId(),
                                k -> new ArrayList<>()
                        )
                        .add(e);
            }
        }

        List<FamilyRow> rows = new ArrayList<>();

        for (var entry : byGuardian.entrySet()) {

            var g = guardians.findById(entry.getKey())
                    .orElse(null);

            if (g == null) {
                continue;
            }

            var gp = g.getPerson();

            String searchable =
                    (
                            gp.getLastName()
                                    + " "
                                    + gp.getFirstName()
                                    + " "
                                    + Optional
                                    .ofNullable(gp.getPhone())
                                    .orElse("")
                    )
                            .toLowerCase(Locale.ROOT);

            if (!q.isBlank() && !searchable.contains(q)) {
                continue;
            }

            List<ChildInfo> children =
                    entry.getValue()
                            .stream()
                            .map(e -> {

                                var s = e.getStudent();
                                var p = s.getPerson();

                                return new ChildInfo(
                                        s.getId(),
                                        s.getStudentNumber(),
                                        p.getLastName(),
                                        p.getFirstName(),
                                        e.getCurrentClassGroupId(),
                                        classNames.get(
                                                e.getCurrentClassGroupId()
                                        )
                                );
                            })
                            .distinct()
                            .toList();

            /*
             * Toutes les créances de la famille :
             *
             * 1. Créances rattachées à l'inscription définitive
             *    (mensualités, autres frais).
             *
             * 2. Frais d'inscription créés auparavant et rattachés
             *    au RegistrationCase.
             */
            List<StudentCharge> familyCharges =
                    new ArrayList<>();

            for (var e : entry.getValue()) {

                // Mensualités et autres frais
                familyCharges.addAll(
                        charges
                                .findByStudentEnrollmentIdOrderByDueDateAscIdAsc(
                                        e.getId()
                                )
                );

                // Frais d'inscription
                registrations
                        .findByStudentIdAndSchoolYearId(
                                e.getStudent().getId(),
                                schoolYearId
                        )
                        .ifPresent(registrationCase ->
                                familyCharges.addAll(
                                        charges
                                                .findByRegistrationCaseIdOrderByDueDateAscIdAsc(
                                                        registrationCase.getId()
                                                )
                                )
                        );
            }

            /*
             * IMPORTANT :
             * On ne réaffecte PAS familyCharges.
             *
             * familyCharges est utilisée dans la lambda ci-dessus,
             * elle doit donc rester effectively final.
             */
            List<StudentCharge> uniqueFamilyCharges =
                    familyCharges.stream()
                            .filter(c ->
                                    !"CANCELLED".equals(c.getStatus())
                            )
                            .collect(
                                    Collectors.toMap(
                                            StudentCharge::getId,
                                            c -> c,
                                            (a, b) -> a,
                                            LinkedHashMap::new
                                    )
                            )
                            .values()
                            .stream()
                            .toList();

            /*
             * FRAIS D'INSCRIPTION
             */
            PaymentCell registration =
                    aggregate(
                            "REGISTRATION",
                            "Inscription",
                            uniqueFamilyCharges.stream()
                                    .filter(this::isRegistration)
                                    .toList(),
                            true
                    );

            /*
             * MENSUALITÉS SEPTEMBRE -> JUIN
             */
            List<PaymentCell> months =
                    new ArrayList<>();

            for (int m : SCHOOL_MONTHS) {

                boolean dueAtCutoff =
                        monthIndex(m) <= monthIndex(cut);

                months.add(
                        aggregate(
                                monthKey(m),
                                monthLabel(m),
                                uniqueFamilyCharges.stream()
                                        .filter(c ->
                                                isMonthly(c, m)
                                        )
                                        .toList(),
                                dueAtCutoff
                        )
                );
            }

            /*
             * TOTAUX À DATE
             */
            BigDecimal expToDate =
                    registration.expected();

            BigDecimal paidToDate =
                    registration.paid();

            BigDecimal remToDate =
                    registration.remaining();

            for (var c : months) {

                int month =
                        Integer.parseInt(
                                c.key().substring(1)
                        );

                if (monthIndex(month) <= monthIndex(cut)) {

                    expToDate =
                            expToDate.add(c.expected());

                    paidToDate =
                            paidToDate.add(c.paid());

                    remToDate =
                            remToDate.add(c.remaining());
                }
            }

            String global;

            if (remToDate.signum() == 0) {
                global = "PAID";
            } else if (paidToDate.signum() > 0) {
                global = "PARTIAL";
            } else {
                global = "UNPAID";
            }

            if (!wantedStatus.isBlank()
                    && !wantedStatus.equals(global)) {
                continue;
            }

            rows.add(
                    new FamilyRow(
                            g.getId(),
                            gp.getLastName(),
                            gp.getFirstName(),
                            gp.getPhone(),
                            gp.getEmail(),
                            children,
                            registration,
                            months,
                            expToDate,
                            paidToDate,
                            remToDate,
                            global
                    )
            );
        }

        rows.sort(
                Comparator
                        .comparing(
                                FamilyRow::guardianLastName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                FamilyRow::guardianFirstName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        /*
         * TOTAL ATTENDU ANNUEL
         */
        BigDecimal expectedAnnual =
                rows.stream()
                        .map(r ->
                                r.registration()
                                        .expected()
                                        .add(
                                                r.months()
                                                        .stream()
                                                        .map(
                                                                PaymentCell::expected
                                                        )
                                                        .reduce(
                                                                BigDecimal.ZERO,
                                                                BigDecimal::add
                                                        )
                                        )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /*
         * TOTAL ATTENDU À DATE
         */
        BigDecimal expectedToDate =
                rows.stream()
                        .map(FamilyRow::expectedToDate)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /*
         * TOTAL ENCAISSÉ
         */
        BigDecimal collected =
                rows.stream()
                        .map(FamilyRow::paidToDate)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /*
         * RESTE À COUVRIR
         */
        BigDecimal remaining =
                rows.stream()
                        .map(FamilyRow::remainingToDate)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /*
         * IMPAYÉS / RETARDS
         */
        BigDecimal overdue =
                rows.stream()
                        .flatMap(r ->
                                concat(
                                        r.registration(),
                                        r.months()
                                ).stream()
                        )
                        .filter(PaymentCell::overdue)
                        .map(PaymentCell::remaining)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /*
         * AVOIRS DISPONIBLES
         */
        BigDecimal availableCredits =
                rows.stream()
                        .map(r ->
                                credits
                                        .findByGuardianIdAndStatusNotOrderByCreatedAtAsc(
                                                r.guardianId(),
                                                "USED"
                                        )
                                        .stream()
                                        .map(
                                                x ->
                                                        x.getRemainingAmount()
                                        )
                                        .reduce(
                                                BigDecimal.ZERO,
                                                BigDecimal::add
                                        )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /*
         * TAUX DE RECOUVREMENT
         */
        BigDecimal rate =
                expectedToDate.signum() == 0
                        ? BigDecimal.ZERO
                        : collected
                        .multiply(
                                BigDecimal.valueOf(100)
                        )
                        .divide(
                                expectedToDate,
                                2,
                                RoundingMode.HALF_UP
                        );

        long up =
                rows.stream()
                        .filter(r ->
                                r.remainingToDate()
                                        .signum() == 0
                        )
                        .count();

        var summary =
                new GlobalSummary(
                        expectedAnnual,
                        expectedToDate,
                        collected,
                        remaining,
                        overdue,
                        availableCredits,
                        rate,
                        rows.size(),
                        up,
                        rows.size() - up
                );

        return new GlobalFinancialSituationResponse(
                schoolYearId,
                cut,
                summary,
                rows
        );
    }

    private PaymentCell aggregate(
            String key,
            String label,
            List<StudentCharge> cs,
            boolean dueAtCutoff
    ) {

        BigDecimal expected =
                cs.stream()
                        .map(StudentCharge::getFinalAmount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal paid =
                cs.stream()
                        .map(c ->
                                allocations.paidForCharge(
                                        c.getId()
                                )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .min(
                                expected.max(
                                        BigDecimal.ZERO
                                )
                        );

        BigDecimal remaining =
                expected
                        .subtract(paid)
                        .max(BigDecimal.ZERO);

        boolean overdue =
                dueAtCutoff
                        && remaining.signum() > 0
                        && cs.stream()
                        .anyMatch(c ->
                                c.getDueDate() != null
                                        && c.getDueDate()
                                        .isBefore(
                                                LocalDate.now()
                                        )
                        );

        String st;

        if (expected.signum() == 0) {
            st = "NOT_APPLICABLE";
        } else if (!dueAtCutoff) {
            st = "UPCOMING";
        } else if (remaining.signum() == 0) {
            st = "PAID";
        } else if (paid.signum() > 0) {
            st = "PARTIAL";
        } else {
            st = "UNPAID";
        }

        return new PaymentCell(
                key,
                label,
                expected,
                paid,
                remaining,
                st,
                overdue
        );
    }

    private boolean isRegistration(StudentCharge c) {

        var f =
                fees.findById(
                        c.getFeeTypeId()
                ).orElse(null);

        if (f == null) {
            return false;
        }

        String s =
                (
                        f.getCode()
                                + " "
                                + f.getName()
                                + " "
                                + f.getCategory()
                )
                        .toUpperCase(Locale.ROOT);

        return s.contains("INSCRI")
                || s.contains("REGISTR");
    }

    private boolean isMonthly(
            StudentCharge c,
            int month
    ) {

        return !isRegistration(c)
                && c.getBillingPeriodStart() != null
                && c.getBillingPeriodStart()
                .getMonthValue() == month;
    }

    private int currentSchoolMonth() {

        int m =
                LocalDate.now()
                        .getMonthValue();

        return SCHOOL_MONTHS.contains(m)
                ? m
                : (m == 7 || m == 8 ? 6 : 9);
    }

    private int normalizeCutoff(int m) {
        return SCHOOL_MONTHS.contains(m)
                ? m
                : currentSchoolMonth();
    }

    private int monthIndex(int m) {
        return SCHOOL_MONTHS.indexOf(m);
    }

    private String monthKey(int m) {
        return "M" + m;
    }

    private String monthLabel(int m) {

        return Month
                .of(m)
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.FRENCH
                );
    }

    private List<PaymentCell> concat(
            PaymentCell registration,
            List<PaymentCell> months
    ) {

        var list =
                new ArrayList<PaymentCell>();

        list.add(registration);
        list.addAll(months);

        return list;
    }
}