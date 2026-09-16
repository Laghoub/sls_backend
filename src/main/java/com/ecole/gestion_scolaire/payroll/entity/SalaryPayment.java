package com.ecole.gestion_scolaire.payroll.entity;
import com.ecole.gestion_scolaire.finance.entity.*; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.OffsetDateTime;
@Entity @Table(name="salary_payment") public class SalaryPayment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payroll_id") private Payroll payroll;
 @Column(name="payment_date",nullable=false) private OffsetDateTime paymentDate;
 @Column(nullable=false) private BigDecimal amount;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payment_method_id") private PaymentMethod paymentMethod;
 @Column(name="cash_register_session_id") private Long cashRegisterSessionId;
 @Column(length=150) private String reference;
 @Column(nullable=false,length=30) private String status;
 @Column(name="paid_by",nullable=false) private Long paidBy;
 @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @Column(name="cancelled_at") private OffsetDateTime cancelledAt; @Column(name="cancellation_reason") private String cancellationReason;
 @PrePersist void pre(){var n=OffsetDateTime.now();if(paymentDate==null)paymentDate=n;if(createdAt==null)createdAt=n;if(status==null)status="VALIDATED";}
 public Long getId(){return id;} public Payroll getPayroll(){return payroll;} public void setPayroll(Payroll v){payroll=v;} public OffsetDateTime getPaymentDate(){return paymentDate;} public void setPaymentDate(OffsetDateTime v){paymentDate=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public PaymentMethod getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(PaymentMethod v){paymentMethod=v;} public Long getCashRegisterSessionId(){return cashRegisterSessionId;} public void setCashRegisterSessionId(Long v){cashRegisterSessionId=v;} public String getReference(){return reference;} public void setReference(String v){reference=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Long getPaidBy(){return paidBy;} public void setPaidBy(Long v){paidBy=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getCancelledAt(){return cancelledAt;} public String getCancellationReason(){return cancellationReason;}
}
