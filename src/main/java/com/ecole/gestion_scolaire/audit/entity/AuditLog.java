package com.ecole.gestion_scolaire.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name="audit_log")
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="occurred_at",nullable=false,updatable=false) private OffsetDateTime occurredAt;
 @Column(name="user_account_id") private Long userAccountId;
 @Column(name="username",length=100) private String username;
 @Column(name="user_last_name",length=100) private String userLastName;
 @Column(name="user_first_name",length=100) private String userFirstName;
 @Column(name="user_roles",length=500) private String userRoles;
 @Column(name="module",nullable=false,length=100) private String module;
 @Column(name="action",nullable=false,length=100) private String action;
 @Column(name="entity_type",length=120) private String entityType;
 @Column(name="entity_id",length=120) private String entityId;
 @Column(name="description",length=1000) private String description;
 @JdbcTypeCode(SqlTypes.JSON)
 @Column(name="old_values", columnDefinition="jsonb") private String oldValues;
 @JdbcTypeCode(SqlTypes.JSON)
 @Column(name="new_values", columnDefinition="jsonb") private String newValues;
 @Column(name="http_method",length=10) private String httpMethod;
 @Column(name="request_path",length=500) private String requestPath;
 @Column(name="http_status") private Integer httpStatus;
 @Column(name="ip_address",length=100) private String ipAddress;
 @Column(name="request_id",length=100) private String requestId;
 @PrePersist void prePersist(){if(occurredAt==null) occurredAt=OffsetDateTime.now();}
 public Long getId(){return id;} public OffsetDateTime getOccurredAt(){return occurredAt;} public void setOccurredAt(OffsetDateTime v){occurredAt=v;}
 public Long getUserAccountId(){return userAccountId;} public void setUserAccountId(Long v){userAccountId=v;}
 public String getUsername(){return username;} public void setUsername(String v){username=v;}
 public String getUserLastName(){return userLastName;} public void setUserLastName(String v){userLastName=v;}
 public String getUserFirstName(){return userFirstName;} public void setUserFirstName(String v){userFirstName=v;}
 public String getUserRoles(){return userRoles;} public void setUserRoles(String v){userRoles=v;}
 public String getModule(){return module;} public void setModule(String v){module=v;} public String getAction(){return action;} public void setAction(String v){action=v;}
 public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;} public String getEntityId(){return entityId;} public void setEntityId(String v){entityId=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getOldValues(){return oldValues;} public void setOldValues(String v){oldValues=v;} public String getNewValues(){return newValues;} public void setNewValues(String v){newValues=v;}
 public String getHttpMethod(){return httpMethod;} public void setHttpMethod(String v){httpMethod=v;} public String getRequestPath(){return requestPath;} public void setRequestPath(String v){requestPath=v;}
 public Integer getHttpStatus(){return httpStatus;} public void setHttpStatus(Integer v){httpStatus=v;} public String getIpAddress(){return ipAddress;} public void setIpAddress(String v){ipAddress=v;} public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=v;}
}
