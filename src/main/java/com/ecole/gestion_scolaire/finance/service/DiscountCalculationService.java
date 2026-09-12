package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.entity.*;
import com.ecole.gestion_scolaire.finance.enums.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.entity.StudentEnrollment;
import com.ecole.gestion_scolaire.registration.repository.StudentEnrollmentRepository;
import com.ecole.gestion_scolaire.school.repository.ClassGroupRepository;
import com.ecole.gestion_scolaire.student.repository.StudentGuardianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class DiscountCalculationService {
    private final StudentDiscountRepository studentDiscounts;
    private final DiscountRuleRepository rules;
    private final StudentEnrollmentRepository enrollments;
    private final ClassGroupRepository classes;
    private final StudentGuardianRepository guardianLinks;

    public DiscountCalculationService(StudentDiscountRepository s, DiscountRuleRepository r,
                                      StudentEnrollmentRepository e, ClassGroupRepository c,
                                      StudentGuardianRepository g) {
        studentDiscounts=s; rules=r; enrollments=e; classes=c; guardianLinks=g;
    }

    public record Result(BigDecimal discountAmount, Long studentDiscountId, Long discountRuleId) {}

    @Transactional(readOnly=true)
    public Result calculate(Long enrollmentId, Long feeTypeId, BigDecimal originalAmount, LocalDate date) {
        if (originalAmount == null || originalAmount.signum() <= 0) return new Result(BigDecimal.ZERO,null,null);
        StudentEnrollment enrollment = enrollments.findById(enrollmentId).orElse(null);
        if (enrollment == null) return new Result(BigDecimal.ZERO,null,null);
        LocalDate d = date == null ? LocalDate.now() : date;

        Candidate best = null;
        for (StudentDiscount sd : studentDiscounts.findByStudentEnrollmentIdAndStatus(enrollmentId, DiscountStatus.APPROVED.name())) {
            if (!dateMatches(sd.getStartDate(), sd.getEndDate(), d)) continue;
            if (sd.getFeeTypeId()!=null && !sd.getFeeTypeId().equals(feeTypeId)) continue;
            BigDecimal amount = amount(sd.getDiscountType(), sd.getValue(), originalAmount);
            if (best==null || amount.compareTo(best.amount)>0) best = new Candidate(amount, sd.getId(), null, Integer.MAX_VALUE);
        }
        for (DiscountRule rule : rules.findBySchoolYearIdAndActiveTrueOrderByPriorityDescIdAsc(enrollment.getSchoolYearId())) {
            if (rule.getFeeTypeId()!=null && !rule.getFeeTypeId().equals(feeTypeId)) continue;
            if (!dateMatches(rule.getValidFrom(), rule.getValidUntil(), d)) continue;
            if (!scopeMatches(rule,enrollment)) continue;
            if (!conditionMatches(rule,enrollment)) continue;
            BigDecimal amount = amount(rule.getDiscountType(), rule.getValue(), originalAmount);
            int pr = rule.getPriority()==null?0:rule.getPriority();
            if (best==null || amount.compareTo(best.amount)>0 || (amount.compareTo(best.amount)==0 && pr>best.priority))
                best = new Candidate(amount,null,rule.getId(),pr);
        }
        if (best==null) return new Result(BigDecimal.ZERO,null,null);
        return new Result(best.amount.min(originalAmount).max(BigDecimal.ZERO),best.studentDiscountId,best.discountRuleId);
    }

    private boolean scopeMatches(DiscountRule rule, StudentEnrollment e){
        var cg=classes.findById(e.getCurrentClassGroupId()).orElse(null); if(cg==null)return false;
        return (rule.getClassGroupId()==null||rule.getClassGroupId().equals(cg.getId()))
                &&(rule.getLevelId()==null||rule.getLevelId().equals(cg.getLevel().getId()))
                &&(rule.getCycleId()==null||rule.getCycleId().equals(cg.getLevel().getCycle().getId()))
                &&(rule.getCampusId()==null||rule.getCampusId().equals(cg.getCampus().getId()));
    }
    private boolean conditionMatches(DiscountRule rule, StudentEnrollment e){
        String t=rule.getConditionType()==null?DiscountConditionType.ALWAYS.name():rule.getConditionType();
        if(DiscountConditionType.ALWAYS.name().equals(t)) return true;
        if(DiscountConditionType.FAMILY_CHILD_RANK.name().equals(t)) {
            if(rule.getChildRank()==null||rule.getChildRank()<1)return false;
            var links=guardianLinks.findByStudentIdOrderByIdAsc(e.getStudent().getId()).stream().filter(x->x.isActive()&&x.isFinancialResponsible()).toList();
            if(links.isEmpty()) links=guardianLinks.findByStudentIdOrderByIdAsc(e.getStudent().getId()).stream().filter(x->x.isActive()).toList();
            if(links.isEmpty()) return false;
            Long guardianId=links.get(0).getGuardian().getId();
            var studentIds=guardianLinks.findByGuardianIdAndActiveTrueOrderByIdAsc(guardianId).stream().map(x->x.getStudent().getId()).distinct().toList();
            var enrolled=studentIds.stream().filter(id->enrollments.findByStudentIdAndSchoolYearId(id,e.getSchoolYearId()).isPresent()).toList();
            int rank=enrolled.indexOf(e.getStudent().getId())+1;
            return rank==rule.getChildRank();
        }
        return false;
    }
    private BigDecimal amount(String type, BigDecimal value, BigDecimal original){
        if(value==null)return BigDecimal.ZERO;
        if(DiscountType.PERCENTAGE.name().equals(type)) return original.multiply(value).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        if(DiscountType.FIXED_AMOUNT.name().equals(type)) return value;
        if(DiscountType.NEW_AMOUNT.name().equals(type)) return original.subtract(value);
        return BigDecimal.ZERO;
    }
    private boolean dateMatches(LocalDate from, LocalDate to, LocalDate d){ return (from==null||!d.isBefore(from))&&(to==null||!d.isAfter(to)); }
    private record Candidate(BigDecimal amount, Long studentDiscountId, Long discountRuleId, int priority){}
}
