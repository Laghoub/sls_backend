package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.dashboard.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class FinanceDashboardAnalyticsService {

    private final NamedParameterJdbcTemplate jdbc;

    public FinanceDashboardAnalyticsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public AdvancedFinanceDashboardResponse dashboard(
            Long schoolYearId,
            Long cycleId,
            Long levelId,
            Long classGroupId,
            Long campusId,
            Long feeTypeId,
            Long paymentMethodId,
            LocalDate from,
            LocalDate to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La date de début doit être antérieure ou égale à la date de fin.");
        }

        var params = params(schoolYearId, cycleId, levelId, classGroupId, campusId, feeTypeId, paymentMethodId, from, to);
        String scope = scopeCte(schoolYearId, cycleId, levelId, classGroupId, campusId, feeTypeId, from, to);

        var kpis = loadKpis(scope, params, paymentMethodId, from, to);
        var cash = loadCash(params, from, to);
        var emails = loadEmails(params, from, to);

        return new AdvancedFinanceDashboardResponse(
                new FinanceDashboardFilterResponse(schoolYearId, cycleId, levelId, classGroupId, campusId, feeTypeId, paymentMethodId, from, to),
                kpis,
                cash,
                emails,
                breakdown(scope, params, "ft.id", "ft.code", "ft.name", "JOIN fee_type ft ON ft.id = cs.fee_type_id", paymentMethodId),
                breakdown(scope, params, "cy.id", "cy.code", "cy.name", "LEFT JOIN cycle cy ON cy.id = cs.cycle_id", paymentMethodId),
                breakdown(scope, params, "lv.id", "lv.code", "lv.name", "LEFT JOIN level lv ON lv.id = cs.level_id", paymentMethodId),
                breakdown(scope, params, "cg.id", "cg.code", "cg.name", "LEFT JOIN class_group cg ON cg.id = cs.class_group_id", paymentMethodId),
                breakdown(scope, params, "cp.id", "cp.code", "cp.name", "LEFT JOIN campus cp ON cp.id = cs.campus_id", paymentMethodId),
                paymentMethods(scope, params, paymentMethodId, from, to),
                monthly(scope, params, paymentMethodId, from, to),
                statuses(scope, params),
                topDebtors(scope, params)
        );
    }

    private MapSqlParameterSource params(Long schoolYearId, Long cycleId, Long levelId, Long classGroupId,
                                         Long campusId, Long feeTypeId, Long paymentMethodId,
                                         LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("schoolYearId", schoolYearId)
                .addValue("cycleId", cycleId)
                .addValue("levelId", levelId)
                .addValue("classGroupId", classGroupId)
                .addValue("campusId", campusId)
                .addValue("feeTypeId", feeTypeId)
                .addValue("paymentMethodId", paymentMethodId)
                .addValue("fromDate", from == null ? null : Date.valueOf(from))
                .addValue("toDate", to == null ? null : Date.valueOf(to));
    }

    /**
     * Construit une vue analytique unique des créances, qu'elles proviennent d'une inscription
     * ou d'une scolarisation annuelle. Les dimensions classe/niveau/cycle/campus viennent de la
     * classe courante pour un élève scolarisé, ou de la classe/niveau demandé pour une inscription.
     */
    private String scopeCte(Long schoolYearId, Long cycleId, Long levelId, Long classGroupId,
                            Long campusId, Long feeTypeId, LocalDate from, LocalDate to) {
        StringBuilder where = new StringBuilder(" WHERE sc.status <> 'CANCELLED' ");
        if (schoolYearId != null) where.append(" AND COALESCE(se.school_year_id, rc.school_year_id) = :schoolYearId ");
        if (cycleId != null) where.append(" AND COALESCE(lv_enr.cycle_id, lv_reg.cycle_id) = :cycleId ");
        if (levelId != null) where.append(" AND COALESCE(cg_enr.level_id, rc.requested_level_id) = :levelId ");
        if (classGroupId != null) where.append(" AND COALESCE(se.current_class_group_id, rc.requested_class_group_id) = :classGroupId ");
        if (campusId != null) where.append(" AND COALESCE(cg_enr.campus_id, cg_reg.campus_id) = :campusId ");
        if (feeTypeId != null) where.append(" AND sc.fee_type_id = :feeTypeId ");
        if (from != null) where.append(" AND COALESCE(sc.due_date, sc.created_at::date) >= :fromDate ");
        if (to != null) where.append(" AND COALESCE(sc.due_date, sc.created_at::date) <= :toDate ");

        return """
                WITH charge_scope AS (
                    SELECT
                        sc.id AS charge_id,
                        sc.fee_type_id,
                        sc.original_amount,
                        sc.discount_amount,
                        sc.final_amount,
                        sc.due_date,
                        sc.status,
                        sc.created_at,
                        COALESCE(se.school_year_id, rc.school_year_id) AS school_year_id,
                        COALESCE(se.student_id, rc.student_id) AS student_id,
                        COALESCE(rc.guardian_id, sg_fin.guardian_id) AS guardian_id,
                        COALESCE(se.current_class_group_id, rc.requested_class_group_id) AS class_group_id,
                        COALESCE(cg_enr.level_id, rc.requested_level_id) AS level_id,
                        COALESCE(lv_enr.cycle_id, lv_reg.cycle_id) AS cycle_id,
                        COALESCE(cg_enr.campus_id, cg_reg.campus_id) AS campus_id
                    FROM student_charge sc
                    LEFT JOIN student_enrollment se ON se.id = sc.student_enrollment_id
                    LEFT JOIN registration_case rc ON rc.id = sc.registration_case_id
                    LEFT JOIN class_group cg_enr ON cg_enr.id = se.current_class_group_id
                    LEFT JOIN level lv_enr ON lv_enr.id = cg_enr.level_id
                    LEFT JOIN class_group cg_reg ON cg_reg.id = rc.requested_class_group_id
                    LEFT JOIN level lv_reg ON lv_reg.id = rc.requested_level_id
                    LEFT JOIN LATERAL (
                        SELECT sg.guardian_id
                        FROM student_guardian sg
                        WHERE sg.student_id = se.student_id
                          AND sg.active = TRUE
                        ORDER BY CASE WHEN sg.financial_responsible THEN 0 ELSE 1 END,
                                 CASE WHEN sg.primary_contact THEN 0 ELSE 1 END,
                                 sg.id
                        LIMIT 1
                    ) sg_fin ON TRUE
                """ + where + "\n),\n" +
                "paid_by_charge AS (\n" +
                "  SELECT cs.charge_id,\n" +
                "         COALESCE(SUM(pa.amount - COALESCE(ra.refunded,0)),0) + COALESCE(dc.direct_credit,0) AS paid\n" +
                "  FROM charge_scope cs\n" +
                "  LEFT JOIN payment_allocation pa ON pa.student_charge_id = cs.charge_id\n" +
                "  LEFT JOIN payment p ON p.id = pa.payment_id AND p.status = 'VALIDATED'\n" +
                "  LEFT JOIN LATERAL (SELECT COALESCE(SUM(x.amount),0) refunded FROM refund_allocation x JOIN refund rr ON rr.id=x.refund_id AND rr.status='VALIDATED' WHERE x.payment_allocation_id = pa.id) ra ON TRUE\n" +
                "  LEFT JOIN LATERAL (SELECT COALESCE(SUM(u.amount),0) direct_credit FROM family_credit_usage u WHERE u.student_charge_id = cs.charge_id AND u.payment_allocation_id IS NULL) dc ON TRUE\n" +
                "  WHERE pa.id IS NULL OR p.id IS NOT NULL\n" +
                "  GROUP BY cs.charge_id, dc.direct_credit\n" +
                ")\n";
    }

    private FinanceDashboardKpiResponse loadKpis(String scope, MapSqlParameterSource params,
                                                  Long paymentMethodId, LocalDate from, LocalDate to) {
        String paymentFilter = paymentMethodId == null ? "" : " AND p.payment_method_id = :paymentMethodId ";
        String paymentDate = dateWhere("p.payment_date", from, to);
        String refundDate = dateWhere("r.refund_date", from, to);
        String sql = scope + """
                SELECT
                  COALESCE((SELECT SUM(original_amount) FROM charge_scope),0) original_charged,
                  COALESCE((SELECT SUM(discount_amount) FROM charge_scope),0) discounts,
                  COALESCE((SELECT SUM(final_amount) FROM charge_scope),0) net_charged,
                  COALESCE((SELECT SUM(LEAST(cs.final_amount, COALESCE(pb.paid,0))) FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id),0) collected,
                  COALESCE((SELECT SUM(GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)) FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id),0) remaining,
                  COALESCE((SELECT SUM(GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)) FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id WHERE cs.due_date < CURRENT_DATE AND GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)>0),0) overdue,
                  COALESCE((SELECT SUM(p.total_amount) FROM payment p WHERE p.status='VALIDATED' """ + paymentDate + paymentFilter + "),0) gross_payments,\n" +
                "  COALESCE((SELECT SUM(r.amount) FROM refund r JOIN payment p ON p.id=r.payment_id WHERE r.status='VALIDATED' " + refundDate + paymentFilter + "),0) refunds,\n" +
                "  COALESCE((SELECT SUM(fc.remaining_amount) FROM family_credit fc WHERE fc.status <> 'USED'),0) credits,\n" +
                "  (SELECT COUNT(*) FROM charge_scope) charge_count,\n" +
                "  (SELECT COUNT(*) FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id WHERE GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)>0 AND COALESCE(pb.paid,0)=0) due_count,\n" +
                "  (SELECT COUNT(*) FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id WHERE COALESCE(pb.paid,0)>0 AND GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)>0) partial_count,\n" +
                "  (SELECT COUNT(*) FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id WHERE GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)=0) paid_count,\n" +
                "  (SELECT COUNT(*) FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id WHERE cs.due_date<CURRENT_DATE AND GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)>0) overdue_count,\n" +
                "  (SELECT COUNT(*) FROM payment p WHERE p.status='VALIDATED' " + paymentDate + paymentFilter + ") payment_count,\n" +
                "  (SELECT COUNT(*) FROM refund r JOIN payment p ON p.id=r.payment_id WHERE r.status='VALIDATED' " + refundDate + paymentFilter + ") refund_count,\n" +
                "  (SELECT COUNT(DISTINCT student_id) FROM charge_scope WHERE student_id IS NOT NULL) student_count,\n" +
                "  (SELECT COUNT(DISTINCT guardian_id) FROM charge_scope WHERE guardian_id IS NOT NULL) family_count";

        Map<String,Object> row = jdbc.queryForMap(sql, params);
        BigDecimal charged = bd(row.get("net_charged"));
        BigDecimal collected = bd(row.get("collected"));
        BigDecimal gross = bd(row.get("gross_payments"));
        BigDecimal refunds = bd(row.get("refunds"));
        long paymentCount = lng(row.get("payment_count"));
        BigDecimal rate = charged.signum() == 0 ? BigDecimal.ZERO : collected.multiply(BigDecimal.valueOf(100)).divide(charged, 2, RoundingMode.HALF_UP);
        BigDecimal avg = paymentCount == 0 ? BigDecimal.ZERO : gross.divide(BigDecimal.valueOf(paymentCount), 2, RoundingMode.HALF_UP);

        return new FinanceDashboardKpiResponse(
                bd(row.get("original_charged")), bd(row.get("discounts")), charged, collected,
                bd(row.get("remaining")), bd(row.get("overdue")), gross, refunds, gross.subtract(refunds),
                bd(row.get("credits")), rate, avg,
                lng(row.get("charge_count")), lng(row.get("due_count")), lng(row.get("partial_count")), lng(row.get("paid_count")),
                lng(row.get("overdue_count")), paymentCount, lng(row.get("refund_count")), lng(row.get("student_count")), lng(row.get("family_count"))
        );
    }

    private List<FinanceDashboardBreakdownResponse> breakdown(String scope, MapSqlParameterSource params,
                                                               String idExpr, String codeExpr, String labelExpr,
                                                               String join, Long paymentMethodId) {
        String methodPaid = paymentMethodId == null ? "" : " AND p2.payment_method_id = :paymentMethodId ";
        String collectedExpr = paymentMethodId == null
                ? "LEAST(cs.final_amount,COALESCE(pb.paid,0))"
                : "COALESCE(pm_paid.paid,0)";
        String sql = scope + """
                SELECT %s id, %s code, %s label,
                       COALESCE(SUM(cs.final_amount),0) charged,
                       COALESCE(SUM(%s),0) collected,
                       COALESCE(SUM(GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)),0) remaining,
                       COALESCE(SUM(CASE WHEN cs.due_date<CURRENT_DATE THEN GREATEST(cs.final_amount-COALESCE(pb.paid,0),0) ELSE 0 END),0) overdue,
                       COUNT(*) charge_count
                FROM charge_scope cs
                LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id
                %s
                LEFT JOIN LATERAL (
                    SELECT COALESCE(SUM(pa2.amount-COALESCE(ra2.refunded,0)),0) paid
                    FROM payment_allocation pa2
                    JOIN payment p2 ON p2.id=pa2.payment_id AND p2.status='VALIDATED'
                    LEFT JOIN LATERAL (
                        SELECT COALESCE(SUM(x.amount),0) refunded
                        FROM refund_allocation x
                        JOIN refund rr2 ON rr2.id = x.refund_id AND rr2.status = 'VALIDATED'
                        WHERE x.payment_allocation_id=pa2.id
                    ) ra2 ON TRUE
                    WHERE pa2.student_charge_id=cs.charge_id %s
                ) pm_paid ON TRUE
                GROUP BY %s,%s,%s
                ORDER BY charged DESC, label NULLS LAST
                """.formatted(idExpr, codeExpr, labelExpr, collectedExpr, join, methodPaid, idExpr, codeExpr, labelExpr);
        return jdbc.query(sql, params, (rs, n) -> new FinanceDashboardBreakdownResponse(
                rs.getObject("id") == null ? null : rs.getLong("id"), rs.getString("code"), rs.getString("label"),
                rs.getBigDecimal("charged"), rs.getBigDecimal("collected"), rs.getBigDecimal("remaining"), rs.getBigDecimal("overdue"), rs.getLong("charge_count")
        ));
    }

    private List<FinanceDashboardPaymentMethodResponse> paymentMethods(
            String scope,
            MapSqlParameterSource params,
            Long methodId,
            LocalDate from,
            LocalDate to
    ) {
        String methodFilter = methodId == null ? "" : " AND pm.id = :paymentMethodId ";
        String paymentDateFilter = dateWhere("p.payment_date", from, to);

        String sql = scope + """
                , method_stats AS (
                    SELECT
                        pm.id AS id,
                        pm.code AS code,
                        pm.name AS name,
                        COALESCE(SUM(GREATEST(pa.amount - COALESCE(ra.refunded_amount, 0), 0)), 0) AS amount,
                        COUNT(DISTINCT p.id) AS payment_count
                    FROM charge_scope cs
                    JOIN payment_allocation pa
                      ON pa.student_charge_id = cs.charge_id
                    JOIN payment p
                      ON p.id = pa.payment_id
                     AND p.status = 'VALIDATED'
                    JOIN payment_method pm
                      ON pm.id = p.payment_method_id
                    LEFT JOIN LATERAL (
                        SELECT COALESCE(SUM(x.amount), 0) AS refunded_amount
                        FROM refund_allocation x
                        JOIN refund r
                          ON r.id = x.refund_id
                         AND r.status = 'VALIDATED'
                        WHERE x.payment_allocation_id = pa.id
                    ) ra ON TRUE
                    WHERE 1 = 1
                """ + paymentDateFilter + methodFilter + """
                    GROUP BY pm.id, pm.code, pm.name
                )
                SELECT
                    ms.id,
                    ms.code,
                    ms.name,
                    ms.amount,
                    ms.payment_count,
                    CASE
                        WHEN SUM(ms.amount) OVER () = 0 THEN 0
                        ELSE ROUND(ms.amount * 100 / SUM(ms.amount) OVER (), 2)
                    END AS share
                FROM method_stats ms
                ORDER BY ms.amount DESC, ms.name
                """;

        return jdbc.query(sql, params, (rs, n) -> new FinanceDashboardPaymentMethodResponse(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getBigDecimal("amount"),
                rs.getLong("payment_count"),
                rs.getBigDecimal("share")
        ));
    }

    /**
     * Evolution mensuelle :
     * - charged    = créances nettes dont l'échéance/création tombe dans le mois ;
     * - collected  = montants BRUTS réellement ventilés par des paiements validés dans le mois ;
     * - refunds    = contrepassations de ces ventilations, à la date réelle du remboursement ;
     * - netCollected = collected - refunds.
     *
     * Important : on ne soustrait PAS les remboursements dans collected puis une seconde fois
     * dans netCollected. Cela évite le double retrait qui existait dans la première version.
     */
    private List<FinanceDashboardMonthlyTrendResponse> monthly(
            String scope,
            MapSqlParameterSource params,
            Long methodId,
            LocalDate from,
            LocalDate to
    ) {
        String paymentMethodFilter = methodId == null ? "" : " AND p.payment_method_id = :paymentMethodId ";
        String paymentDateFilter = dateWhere("p.payment_date", from, to);
        String refundDateFilter = dateWhere("r.refund_date", from, to);

        String sql = scope + """
                , charge_months AS (
                    SELECT
                        date_trunc('month', COALESCE(cs.due_date, cs.created_at::date))::date AS month_start,
                        COALESCE(SUM(cs.final_amount), 0) AS charged
                    FROM charge_scope cs
                    GROUP BY date_trunc('month', COALESCE(cs.due_date, cs.created_at::date))::date
                ), payment_months AS (
                    SELECT
                        date_trunc('month', p.payment_date)::date AS month_start,
                        COALESCE(SUM(pa.amount), 0) AS collected
                    FROM charge_scope cs
                    JOIN payment_allocation pa
                      ON pa.student_charge_id = cs.charge_id
                    JOIN payment p
                      ON p.id = pa.payment_id
                     AND p.status = 'VALIDATED'
                    WHERE 1 = 1
                """ + paymentDateFilter + paymentMethodFilter + """
                    GROUP BY date_trunc('month', p.payment_date)::date
                ), refund_months AS (
                    SELECT
                        date_trunc('month', r.refund_date)::date AS month_start,
                        COALESCE(SUM(ra.amount), 0) AS refunds
                    FROM charge_scope cs
                    JOIN refund_allocation ra
                      ON ra.student_charge_id = cs.charge_id
                    JOIN refund r
                      ON r.id = ra.refund_id
                     AND r.status = 'VALIDATED'
                    JOIN payment p
                      ON p.id = r.payment_id
                    WHERE 1 = 1
                """ + refundDateFilter + paymentMethodFilter + """
                    GROUP BY date_trunc('month', r.refund_date)::date
                ), all_months AS (
                    SELECT cm.month_start FROM charge_months cm
                    UNION
                    SELECT pm.month_start FROM payment_months pm
                    UNION
                    SELECT rm.month_start FROM refund_months rm
                )
                SELECT
                    am.month_start AS month_start,
                    COALESCE(cm.charged, 0) AS charged,
                    COALESCE(pm.collected, 0) AS collected,
                    COALESCE(rm.refunds, 0) AS refunds,
                    COALESCE(pm.collected, 0) - COALESCE(rm.refunds, 0) AS net_collected
                FROM all_months am
                LEFT JOIN charge_months cm
                  ON cm.month_start = am.month_start
                LEFT JOIN payment_months pm
                  ON pm.month_start = am.month_start
                LEFT JOIN refund_months rm
                  ON rm.month_start = am.month_start
                ORDER BY am.month_start
                """;

        return jdbc.query(sql, params, (rs, n) -> new FinanceDashboardMonthlyTrendResponse(
                rs.getDate("month_start").toLocalDate(),
                rs.getBigDecimal("charged"),
                rs.getBigDecimal("collected"),
                rs.getBigDecimal("refunds"),
                rs.getBigDecimal("net_collected")
        ));
    }

    private List<FinanceDashboardStatusResponse> statuses(String scope, MapSqlParameterSource params) {
        String sql = scope + """
                SELECT CASE
                         WHEN GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)=0 THEN 'PAID'
                         WHEN COALESCE(pb.paid,0)>0 THEN 'PARTIALLY_PAID'
                         WHEN cs.due_date<CURRENT_DATE THEN 'OVERDUE'
                         ELSE 'DUE'
                       END status,
                       COUNT(*) count,
                       SUM(cs.final_amount) amount,
                       SUM(GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)) remaining
                FROM charge_scope cs LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id
                GROUP BY 1 ORDER BY amount DESC
                """;
        return jdbc.query(sql, params, (rs,n) -> new FinanceDashboardStatusResponse(
                rs.getString("status"), rs.getLong("count"), rs.getBigDecimal("amount"), rs.getBigDecimal("remaining")
        ));
    }

    private List<FinanceDashboardDebtorResponse> topDebtors(String scope, MapSqlParameterSource params) {
        String sql = scope + """
                SELECT cs.guardian_id,
                       TRIM(COALESCE(pr.first_name,'') || ' ' || COALESCE(pr.last_name,'')) guardian_name,
                       pr.phone,
                       COUNT(DISTINCT cs.student_id) student_count,
                       SUM(GREATEST(cs.final_amount-COALESCE(pb.paid,0),0)) remaining,
                       SUM(CASE WHEN cs.due_date<CURRENT_DATE THEN GREATEST(cs.final_amount-COALESCE(pb.paid,0),0) ELSE 0 END) overdue
                FROM charge_scope cs
                LEFT JOIN paid_by_charge pb ON pb.charge_id=cs.charge_id
                LEFT JOIN guardian g ON g.id=cs.guardian_id
                LEFT JOIN person pr ON pr.id=g.person_id
                WHERE cs.guardian_id IS NOT NULL
                GROUP BY cs.guardian_id,pr.first_name,pr.last_name,pr.phone
                HAVING SUM(GREATEST(cs.final_amount-COALESCE(pb.paid,0),0))>0
                ORDER BY overdue DESC, remaining DESC
                LIMIT 10
                """;
        return jdbc.query(sql, params, (rs,n) -> new FinanceDashboardDebtorResponse(
                rs.getLong("guardian_id"), rs.getString("guardian_name"), rs.getString("phone"), rs.getLong("student_count"),
                rs.getBigDecimal("remaining"), rs.getBigDecimal("overdue")
        ));
    }

    private FinanceDashboardCashResponse loadCash(MapSqlParameterSource params, LocalDate from, LocalDate to) {
        String movementDates = dateWhere("cm.created_at", from, to);
        String sessionDates = dateWhere("cs.opened_at", from, to);
        String sql = """
                SELECT
                  COALESCE((SELECT SUM(cs.opening_balance) FROM cash_register_session cs WHERE 1=1 %s),0) opening_funds,
                  COALESCE((SELECT SUM(cm.amount) FROM cash_movement cm WHERE cm.direction='IN' %s),0) inflows,
                  COALESCE((SELECT SUM(cm.amount) FROM cash_movement cm WHERE cm.direction='OUT' %s),0) outflows,
                  COALESCE((SELECT SUM(cs.expected_closing_balance) FROM cash_register_session cs WHERE cs.status='CLOSED' %s),0) expected_total,
                  COALESCE((SELECT SUM(cs.actual_closing_balance) FROM cash_register_session cs WHERE cs.status='CLOSED' %s),0) actual_total,
                  COALESCE((SELECT SUM(cs.difference_amount) FROM cash_register_session cs WHERE cs.status='CLOSED' %s),0) difference_total,
                  (SELECT COUNT(*) FROM cash_register_session cs WHERE cs.status='OPEN' %s) open_count,
                  (SELECT COUNT(*) FROM cash_register_session cs WHERE cs.status='CLOSED' %s) closed_count
                """.formatted(sessionDates,movementDates,movementDates,sessionDates,sessionDates,sessionDates,sessionDates,sessionDates);
        Map<String,Object> row=jdbc.queryForMap(sql,params);
        BigDecimal in=bd(row.get("inflows")), out=bd(row.get("outflows"));
        return new FinanceDashboardCashResponse(bd(row.get("opening_funds")),in,out,in.subtract(out),bd(row.get("expected_total")),bd(row.get("actual_total")),bd(row.get("difference_total")),lng(row.get("open_count")),lng(row.get("closed_count")));
    }

    private FinanceDashboardEmailResponse loadEmails(MapSqlParameterSource params, LocalDate from, LocalDate to) {
        String dates=dateWhere("e.created_at",from,to);
        String sql="SELECT COUNT(*) FILTER(WHERE e.status='SENT') sent, COUNT(*) FILTER(WHERE e.status='PENDING' AND e.attempt_count=0) pending, COUNT(*) FILTER(WHERE e.status<>'SENT' AND e.attempt_count>0) retrying FROM finance_email_outbox e WHERE 1=1 "+dates;
        Map<String,Object> row=jdbc.queryForMap(sql,params);
        return new FinanceDashboardEmailResponse(lng(row.get("sent")),lng(row.get("pending")),lng(row.get("retrying")));
    }

    private String dateWhere(String column, LocalDate from, LocalDate to) {
        StringBuilder s=new StringBuilder();
        if(from!=null)s.append(" AND ").append(column).append("::date >= :fromDate ");
        if(to!=null)s.append(" AND ").append(column).append("::date <= :toDate ");
        return s.toString();
    }

    private BigDecimal bd(Object v){return v==null?BigDecimal.ZERO:(v instanceof BigDecimal b?b:new BigDecimal(v.toString()));}
    private long lng(Object v){return v==null?0L:((Number)v).longValue();}
}
