package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.tracking.*;
import com.ecole.gestion_scolaire.finance.entity.StudentCharge;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.repository.*;
import com.ecole.gestion_scolaire.student.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FinancialTrackingService {
    private final StudentRepository students; private final GuardianRepository guardians; private final StudentGuardianRepository links;
    private final StudentEnrollmentRepository enrollments; private final RegistrationCaseRepository registrations;
    private final StudentChargeRepository charges; private final FeeTypeRepository fees; private final PaymentAllocationService allocations;
    private final FamilyCreditRepository credits;
    public FinancialTrackingService(StudentRepository s, GuardianRepository g, StudentGuardianRepository l, StudentEnrollmentRepository e,
                                    RegistrationCaseRepository r, StudentChargeRepository c, FeeTypeRepository f, PaymentAllocationService a, FamilyCreditRepository cr){
        students=s;guardians=g;links=l;enrollments=e;registrations=r;charges=c;fees=f;allocations=a;credits=cr;
    }

    @Transactional(readOnly=true)
    public StudentFinancialSituationResponse student(Long studentId, Long schoolYearId){
        var st=students.findById(studentId).orElseThrow(()->new FinanceNotFoundException("Élève introuvable."));
        var en=enrollments.findByStudentIdAndSchoolYearId(studentId,schoolYearId).orElse(null);
        var reg=registrations.findByStudentIdAndSchoolYearId(studentId,schoolYearId).orElse(null);
        var list=new ArrayList<StudentCharge>();
        if(en!=null) list.addAll(charges.findByStudentEnrollmentIdOrderByDueDateAscIdAsc(en.getId()));
        if(reg!=null) list.addAll(charges.findByRegistrationCaseIdOrderByDueDateAscIdAsc(reg.getId()));
        var lines=list.stream().filter(x->!"CANCELLED".equals(x.getStatus())).map(this::line).toList();
        BigDecimal total=lines.stream().map(FinancialChargeLineResponse::finalAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal paid=lines.stream().map(FinancialChargeLineResponse::paidAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal rem=lines.stream().map(FinancialChargeLineResponse::remainingAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal late=lines.stream().filter(FinancialChargeLineResponse::overdue).map(FinancialChargeLineResponse::remainingAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        var p=st.getPerson();
        return new StudentFinancialSituationResponse(st.getId(),st.getStudentNumber(),p.getLastName(),p.getFirstName(),schoolYearId,en==null?null:en.getId(),en==null?null:en.getCurrentClassGroupId(),total,paid,rem,late,lines);
    }

    @Transactional(readOnly=true)
    public FamilyFinancialSituationResponse family(Long guardianId, Long schoolYearId){
        var g=guardians.findById(guardianId).orElseThrow(()->new FinanceNotFoundException("Responsable introuvable."));
        var ss=links.findByGuardianIdAndActiveTrueOrderByIdAsc(guardianId).stream().map(x->student(x.getStudent().getId(),schoolYearId)).toList();
        BigDecimal total=ss.stream().map(StudentFinancialSituationResponse::totalCharged).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal paid=ss.stream().map(StudentFinancialSituationResponse::totalPaid).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal rem=ss.stream().map(StudentFinancialSituationResponse::totalRemaining).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal late=ss.stream().map(StudentFinancialSituationResponse::overdueAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal credit=credits.findByGuardianIdAndStatusNotOrderByCreatedAtAsc(guardianId,"USED").stream().map(x->x.getRemainingAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);
        var p=g.getPerson(); return new FamilyFinancialSituationResponse(g.getId(),p.getLastName(),p.getFirstName(),p.getPhone(),p.getEmail(),schoolYearId,total,paid,rem,late,credit,ss);
    }

    @Transactional(readOnly=true)
    public PageResponse<OverdueChargeResponse> overdue(int page,int size){
        var all=charges.findByStatusInAndDueDateBeforeOrderByDueDateAsc(List.of("DUE","PARTIALLY_PAID"),LocalDate.now()).stream().map(this::overdueLine).filter(Objects::nonNull).toList();
        int ps=Math.min(100,Math.max(1,size)), from=Math.min(Math.max(0,page)*ps,all.size()), to=Math.min(from+ps,all.size());
        var slice=all.subList(from,to); int totalPages=(int)Math.ceil(all.size()/(double)ps);
        return new PageResponse<>(slice,page,ps,all.size(),totalPages,page==0,to==all.size());
    }

    @Transactional(readOnly=true)
    public FinanceDashboardResponse dashboard(Long schoolYearId){
        var studentIds=enrollments.findBySchoolYearId(schoolYearId,Pageable.unpaged()).getContent().stream().map(x->x.getStudent().getId()).toList();
        var situations=studentIds.stream().distinct().map(id->student(id,schoolYearId)).toList();
        BigDecimal total=situations.stream().map(StudentFinancialSituationResponse::totalCharged).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal paid=situations.stream().map(StudentFinancialSituationResponse::totalPaid).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal rem=situations.stream().map(StudentFinancialSituationResponse::totalRemaining).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal late=situations.stream().map(StudentFinancialSituationResponse::overdueAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        var lines=situations.stream().flatMap(s->s.charges().stream()).toList();
        long due=lines.stream().filter(x->"DUE".equals(x.status())).count(), part=lines.stream().filter(x->"PARTIALLY_PAID".equals(x.status())).count(), ok=lines.stream().filter(x->"PAID".equals(x.status())).count(), lateCount=lines.stream().filter(FinancialChargeLineResponse::overdue).count();
        BigDecimal credit=credits.findAll().stream().map(x->x.getRemainingAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new FinanceDashboardResponse(schoolYearId,total,paid,rem,late,due,part,ok,lateCount,credit);
    }

    private FinancialChargeLineResponse line(StudentCharge c){ var fee=fees.findById(c.getFeeTypeId()).orElse(null); var paid=allocations.paidForCharge(c.getId()); var rem=c.getFinalAmount().subtract(paid).max(BigDecimal.ZERO); boolean late=c.getDueDate()!=null&&c.getDueDate().isBefore(LocalDate.now())&&rem.signum()>0; return new FinancialChargeLineResponse(c.getId(),c.getFeeTypeId(),fee==null?null:fee.getCode(),fee==null?null:fee.getName(),c.getLabel(),c.getFinalAmount(),paid,rem,c.getDueDate(),c.getBillingPeriodStart(),c.getBillingPeriodEnd(),c.getStatus(),late); }
    private OverdueChargeResponse overdueLine(StudentCharge c){ Long sid=null,gid=null; if(c.getStudentEnrollmentId()!=null){ var e=enrollments.findById(c.getStudentEnrollmentId()).orElse(null); if(e!=null)sid=e.getStudent().getId(); } else if(c.getRegistrationCaseId()!=null){ var r=registrations.findById(c.getRegistrationCaseId()).orElse(null); if(r!=null){sid=r.getStudent().getId();gid=r.getGuardian().getId();} } if(sid==null)return null; var st=students.findById(sid).orElse(null); if(st==null)return null; if(gid==null) gid=links.findByStudentIdOrderByIdAsc(sid).stream().filter(x->x.isActive()&&x.isFinancialResponsible()).map(x->x.getGuardian().getId()).findFirst().orElse(null); var g=gid==null?null:guardians.findById(gid).orElse(null); var paid=allocations.paidForCharge(c.getId());var rem=c.getFinalAmount().subtract(paid).max(BigDecimal.ZERO); return new OverdueChargeResponse(c.getId(),sid,st.getStudentNumber(),st.getPerson().getLastName(),st.getPerson().getFirstName(),gid,g==null?null:g.getPerson().getLastName(),g==null?null:g.getPerson().getFirstName(),g==null?null:g.getPerson().getPhone(),c.getLabel(),c.getDueDate(),c.getFinalAmount(),paid,rem,ChronoUnit.DAYS.between(c.getDueDate(),LocalDate.now()),c.getStatus()); }
}
