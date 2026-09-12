package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "finance_email_outbox")
public class FinanceEmailOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="recipient_email", nullable=false, length=320) private String recipientEmail;
    @Column(nullable=false, length=250) private String subject;
    @Column(name="body_html", nullable=false, columnDefinition="text") private String bodyHtml;
    @Column(name="document_type", nullable=false, length=40) private String documentType;
    @Column(name="document_id", nullable=false) private Long documentId;
    @Column(nullable=false, length=30) private String status;
    @Column(name="attempt_count", nullable=false) private int attemptCount;
    @Column(name="last_error", columnDefinition="text") private String lastError;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @Column(name="sent_at") private OffsetDateTime sentAt;
    @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now(); if(status==null)status="PENDING";}
    public Long getId(){return id;} public String getRecipientEmail(){return recipientEmail;} public void setRecipientEmail(String v){recipientEmail=v;}
    public String getSubject(){return subject;} public void setSubject(String v){subject=v;} public String getBodyHtml(){return bodyHtml;} public void setBodyHtml(String v){bodyHtml=v;}
    public String getDocumentType(){return documentType;} public void setDocumentType(String v){documentType=v;} public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public int getAttemptCount(){return attemptCount;} public void setAttemptCount(int v){attemptCount=v;}
    public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getSentAt(){return sentAt;} public void setSentAt(OffsetDateTime v){sentAt=v;}
}
