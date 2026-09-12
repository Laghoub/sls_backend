package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.discount.*;
import com.ecole.gestion_scolaire.finance.entity.*;
import com.ecole.gestion_scolaire.finance.enums.*;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.repository.StudentEnrollmentRepository;
import com.ecole.gestion_scolaire.student.repository.StudentGuardianRepository;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class DiscountService {
 private final StudentDiscountRepository repo; private final StudentDiscountAdjustmentRepository adjustments;
 private final StudentChargeRepository charges; private final StudentEnrollmentRepository enrollments; private final FeeTypeRepository fees;
 private final DiscountCalculationService calculator; private final PaymentAllocationService allocations; private final FamilyCreditService credits;
 private final StudentGuardianRepository guardianLinks; private final CurrentFinanceAccountService current;
 public DiscountService(StudentDiscountRepository r,StudentDiscountAdjustmentRepository a,StudentChargeRepository c,StudentEnrollmentRepository e,FeeTypeRepository f,DiscountCalculationService calc,PaymentAllocationService pa,FamilyCreditService fc,StudentGuardianRepository gl,CurrentFinanceAccountService cu){repo=r;adjustments=a;charges=c;enrollments=e;fees=f;calculator=calc;allocations=pa;credits=fc;guardianLinks=gl;current=cu;}

 @Transactional
 public StudentDiscountResponse create(StudentDiscountRequest r){
   enrollments.findById(r.studentEnrollmentId()).orElseThrow(()->new FinanceNotFoundException("Scolarisation introuvable."));
   if(r.feeTypeId()!=null&&!fees.existsById(r.feeTypeId()))throw new FinanceNotFoundException("Type de frais introuvable.");
   validateType(r.discountType(),r.value()); if(r.endDate()!=null&&r.endDate().isBefore(r.startDate()))throw new FinanceBusinessException("La date de fin est antérieure à la date de début.");
   var x=new StudentDiscount();x.setStudentEnrollmentId(r.studentEnrollmentId());x.setFeeTypeId(r.feeTypeId());x.setDiscountType(r.discountType().toUpperCase(Locale.ROOT));x.setValue(r.value());x.setStartDate(r.startDate());x.setEndDate(r.endDate());x.setReason(r.reason());x.setStatus(DiscountStatus.PENDING_APPROVAL.name());x.setCreatedBy(current.id());
   x=repo.save(x);
   if(canApprove()) return approve(x.getId());
   return toResponse(x);
 }
 @Transactional(readOnly=true) public PageResponse<StudentDiscountResponse> search(String status,Long enrollmentId,int page,int size){var p=PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by("createdAt").descending());Page<StudentDiscount>x=enrollmentId!=null?repo.findByStudentEnrollmentId(enrollmentId,p):status!=null&&!status.isBlank()?repo.findByStatus(status,p):repo.findAll(p);return PageResponse.from(x.map(this::toResponse));}
 @Transactional public StudentDiscountResponse approve(Long id){var x=get(id);if(!DiscountStatus.PENDING_APPROVAL.name().equals(x.getStatus()))throw new FinanceBusinessException("Cette réduction n'est plus en attente de validation.");x.setStatus(DiscountStatus.APPROVED.name());x.setApprovedBy(current.id());x.setApprovedAt(OffsetDateTime.now());repo.save(x);applyEnrollment(x.getStudentEnrollmentId(),x.getId(),null);x.setAppliedAt(OffsetDateTime.now());return toResponse(repo.save(x));}
 @Transactional public StudentDiscountResponse reject(Long id,String reason){var x=get(id);if(!DiscountStatus.PENDING_APPROVAL.name().equals(x.getStatus()))throw new FinanceBusinessException("Cette réduction n'est plus en attente de validation.");x.setStatus(DiscountStatus.REJECTED.name());x.setRejectedBy(current.id());x.setRejectedAt(OffsetDateTime.now());x.setRejectionReason(reason);return toResponse(repo.save(x));}
 @Transactional public DiscountApplicationResponse reapplyEnrollment(Long enrollmentId){enrollments.findById(enrollmentId).orElseThrow(()->new FinanceNotFoundException("Scolarisation introuvable."));return applyEnrollment(enrollmentId,null,null);}

 private DiscountApplicationResponse applyEnrollment(Long enrollmentId,Long sourceDiscountId,Long sourceRuleId){int count=0;BigDecimal total=BigDecimal.ZERO,credit=BigDecimal.ZERO;Long uid=current.id();
   for(var c:charges.findByStudentEnrollmentIdOrderByDueDateAscIdAsc(enrollmentId)){if("CANCELLED".equals(c.getStatus()))continue;LocalDate date=c.getBillingPeriodStart()!=null?c.getBillingPeriodStart():c.getDueDate();var calc=calculator.calculate(enrollmentId,c.getFeeTypeId(),c.getOriginalAmount(),date);BigDecimal newDisc=calc.discountAmount();BigDecimal oldDisc=c.getDiscountAmount();BigDecimal oldFinal=c.getFinalAmount();BigDecimal newFinal=c.getOriginalAmount().subtract(newDisc).max(BigDecimal.ZERO);if(newFinal.compareTo(oldFinal)==0&&newDisc.compareTo(oldDisc)==0)continue;BigDecimal settled=allocations.paidForCharge(c.getId());BigDecimal oldExcess=settled.subtract(oldFinal).max(BigDecimal.ZERO);BigDecimal newExcess=settled.subtract(newFinal).max(BigDecimal.ZERO);BigDecimal extraCredit=newExcess.subtract(oldExcess).max(BigDecimal.ZERO);
     c.setDiscountAmount(newDisc);c.setFinalAmount(newFinal);c.setStatus(settled.signum()==0?"DUE":settled.compareTo(newFinal)>=0?"PAID":"PARTIALLY_PAID");charges.save(c);
     var adj=new StudentDiscountAdjustment();adj.setStudentDiscountId(sourceDiscountId!=null?sourceDiscountId:calc.studentDiscountId());adj.setDiscountRuleId(sourceRuleId!=null?sourceRuleId:calc.discountRuleId());adj.setStudentChargeId(c.getId());adj.setPreviousDiscountAmount(oldDisc);adj.setNewDiscountAmount(newDisc);adj.setPreviousFinalAmount(oldFinal);adj.setNewFinalAmount(newFinal);adj.setCreditCreated(extraCredit);adj.setAppliedBy(uid);adjustments.save(adj);
     if(extraCredit.signum()>0){Long gid=guardianForEnrollment(enrollmentId);credits.createFromDiscount(gid,sourceDiscountId!=null?sourceDiscountId:calc.studentDiscountId(),extraCredit);credit=credit.add(extraCredit);} count++; total=total.add(oldFinal.subtract(newFinal).max(BigDecimal.ZERO)); }
   return new DiscountApplicationResponse(count,total,credit);
 }
 private Long guardianForEnrollment(Long enrollmentId){var e=enrollments.findById(enrollmentId).orElseThrow();var links=guardianLinks.findByStudentIdOrderByIdAsc(e.getStudent().getId()).stream().filter(x->x.isActive()&&x.isFinancialResponsible()).toList();if(links.isEmpty())links=guardianLinks.findByStudentIdOrderByIdAsc(e.getStudent().getId()).stream().filter(x->x.isActive()).toList();if(links.isEmpty())throw new FinanceBusinessException("Aucun responsable actif trouvé pour créer l'avoir familial.");return links.get(0).getGuardian().getId();}
 private StudentDiscount get(Long id){return repo.findById(id).orElseThrow(()->new FinanceNotFoundException("Réduction introuvable : "+id));}
 private boolean canApprove(){var a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getAuthorities().stream().anyMatch(x->"REDUCTION_VALIDER".equals(x.getAuthority())||"ROLE_ADMIN".equals(x.getAuthority()));}
 private void validateType(String t,BigDecimal v){try{var type=DiscountType.valueOf(t.toUpperCase(Locale.ROOT));if(v==null||v.signum()<0)throw new Exception();if(type==DiscountType.PERCENTAGE&&v.compareTo(new BigDecimal("100"))>0)throw new FinanceBusinessException("Le pourcentage ne peut pas dépasser 100 %. ");}catch(IllegalArgumentException e){throw new FinanceBusinessException("Type de réduction invalide. Utilisez PERCENTAGE, FIXED_AMOUNT ou NEW_AMOUNT.");}catch(Exception e){throw new FinanceBusinessException("Valeur de réduction invalide.");}}
 private StudentDiscountResponse toResponse(StudentDiscount x){return new StudentDiscountResponse(x.getId(),x.getStudentEnrollmentId(),x.getFeeTypeId(),x.getDiscountType(),x.getValue(),x.getStartDate(),x.getEndDate(),x.getReason(),x.getStatus(),x.getCreatedBy(),x.getCreatedAt(),x.getApprovedBy(),x.getApprovedAt(),x.getRejectedBy(),x.getRejectedAt(),x.getRejectionReason(),x.getAppliedAt());}
}
