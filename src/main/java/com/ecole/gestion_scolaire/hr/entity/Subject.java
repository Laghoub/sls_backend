package com.ecole.gestion_scolaire.hr.entity;
import jakarta.persistence.*;
@Entity @Table(name="subject")
public class Subject {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=50) private String code;
 @Column(nullable=false,length=150) private String name;
 @Column(nullable=false) private boolean active;
 @Column(name="display_order") private Integer displayOrder;
 public Long getId(){return id;} public void setId(Long v){id=v;} public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;} public Integer getDisplayOrder(){return displayOrder;} public void setDisplayOrder(Integer v){displayOrder=v;}
}