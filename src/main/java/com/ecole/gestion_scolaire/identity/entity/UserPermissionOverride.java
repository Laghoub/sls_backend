package com.ecole.gestion_scolaire.identity.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="user_permission_override", uniqueConstraints=@UniqueConstraint(name="uk_user_permission_override", columnNames={"user_account_id","permission_id"}))
public class UserPermissionOverride {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_account_id",nullable=false) private UserAccount userAccount;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="permission_id",nullable=false) private Permission permission;
    @Column(nullable=false,length=10) private String effect;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @PrePersist void create(){ if(createdAt==null) createdAt=OffsetDateTime.now(); }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public UserAccount getUserAccount(){return userAccount;} public void setUserAccount(UserAccount v){userAccount=v;}
    public Permission getPermission(){return permission;} public void setPermission(Permission v){permission=v;}
    public String getEffect(){return effect;} public void setEffect(String v){effect=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
}
