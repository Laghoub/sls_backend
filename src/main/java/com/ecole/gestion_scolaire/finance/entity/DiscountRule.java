package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="discount_rule")
public class DiscountRule {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="school_year_id",nullable=false) private Long schoolYearId;
 @Column(nullable=false,length=50) private String code;
 @Column(nullable=false,length=150) private String name;
 @Column(name="discount_type",nullable=false,length=30) private String discountType;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal value;
 @Column(name="fee_type_id") private Long feeTypeId;
 @Column(nullable=false) private Integer priority;
 @Column(name="requires_approval",nullable=false) private boolean requiresApproval;
 @Column(nullable=false) private boolean active;
 @Column(name="valid_from") private LocalDate validFrom;
 @Column(name="valid_until") private LocalDate validUntil;
 @Column(name="condition_type",nullable=false,length=40) private String conditionType;
 @Column(name="child_rank") private Integer childRank;
 @Column(name="cycle_id") private Long cycleId;
 @Column(name="level_id") private Long levelId;
 @Column(name="class_group_id") private Long classGroupId;
 @Column(name="campus_id") private Long campusId;
 public Long getId(){return id;} public Long getSchoolYearId(){return schoolYearId;} public void setSchoolYearId(Long v){schoolYearId=v;}
 public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;}
 public String getDiscountType(){return discountType;} public void setDiscountType(String v){discountType=v;} public BigDecimal getValue(){return value;} public void setValue(BigDecimal v){value=v;}
 public Long getFeeTypeId(){return feeTypeId;} public void setFeeTypeId(Long v){feeTypeId=v;} public Integer getPriority(){return priority;} public void setPriority(Integer v){priority=v;}
 public boolean isRequiresApproval(){return requiresApproval;} public void setRequiresApproval(boolean v){requiresApproval=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public LocalDate getValidFrom(){return validFrom;} public void setValidFrom(LocalDate v){validFrom=v;} public LocalDate getValidUntil(){return validUntil;} public void setValidUntil(LocalDate v){validUntil=v;}
 public String getConditionType(){return conditionType;} public void setConditionType(String v){conditionType=v;} public Integer getChildRank(){return childRank;} public void setChildRank(Integer v){childRank=v;}
 public Long getCycleId(){return cycleId;} public void setCycleId(Long v){cycleId=v;} public Long getLevelId(){return levelId;} public void setLevelId(Long v){levelId=v;}
 public Long getClassGroupId(){return classGroupId;} public void setClassGroupId(Long v){classGroupId=v;} public Long getCampusId(){return campusId;} public void setCampusId(Long v){campusId=v;}
}
