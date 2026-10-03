package com.ecole.gestion_scolaire.accountactivation.entity;
import com.ecole.gestion_scolaire.identity.entity.UserAccount; import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="account_activation_token") public class AccountActivationToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_account_id",nullable=false) private UserAccount userAccount;
 @Column(name="token_hash",nullable=false,length=64,unique=true) private String tokenHash;
 @Column(name="expires_at",nullable=false) private OffsetDateTime expiresAt; @Column(name="used_at") private OffsetDateTime usedAt;
 @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now();}
 public Long getId(){return id;} public UserAccount getUserAccount(){return userAccount;} public void setUserAccount(UserAccount v){userAccount=v;}
 public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;} public OffsetDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(OffsetDateTime v){expiresAt=v;}
 public OffsetDateTime getUsedAt(){return usedAt;} public void setUsedAt(OffsetDateTime v){usedAt=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
