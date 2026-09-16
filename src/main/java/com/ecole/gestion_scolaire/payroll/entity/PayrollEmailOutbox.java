package com.ecole.gestion_scolaire.payroll.entity;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="payroll_email_outbox") public class PayrollEmailOutbox {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="recipient_email",nullable=false,length=255) private String recipientEmail;
 @Column(nullable=false,length=300) private String subject;
 @Column(name="body_html",nullable=false,columnDefinition="text") private String bodyHtml;
 @Column(name="salary_payment_id",nullable=false) private Long salaryPaymentId;
 @Column(nullable=false,length=20) private String status="PENDING";
 @Column(name="attempt_count",nullable=false) private int attemptCount;
 @Column(name="last_error",length=2000) private String lastError;
 @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @Column(name="processing_at") private OffsetDateTime processingAt;
 @Column(name="sent_at") private OffsetDateTime sentAt;
 @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now();if(status==null)status="PENDING";}
 public Long getId(){return id;} public String getRecipientEmail(){return recipientEmail;} public void setRecipientEmail(String v){recipientEmail=v;} public String getSubject(){return subject;} public void setSubject(String v){subject=v;} public String getBodyHtml(){return bodyHtml;} public void setBodyHtml(String v){bodyHtml=v;} public Long getSalaryPaymentId(){return salaryPaymentId;} public void setSalaryPaymentId(Long v){salaryPaymentId=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public int getAttemptCount(){return attemptCount;} public void setAttemptCount(int v){attemptCount=v;} public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getProcessingAt(){return processingAt;} public void setProcessingAt(OffsetDateTime v){processingAt=v;} public OffsetDateTime getSentAt(){return sentAt;} public void setSentAt(OffsetDateTime v){sentAt=v;}
}
