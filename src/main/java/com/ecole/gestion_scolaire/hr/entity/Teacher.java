package com.ecole.gestion_scolaire.hr.entity;
import com.ecole.gestion_scolaire.hr.enums.TeacherStatus; import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="teacher")
public class Teacher {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id",nullable=false) private Employee employee;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private TeacherStatus status;
 @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now(); if(status==null)status=TeacherStatus.ACTIVE;}
 public Long getId(){return id;} public void setId(Long v){id=v;} public Employee getEmployee(){return employee;} public void setEmployee(Employee v){employee=v;} public TeacherStatus getStatus(){return status;} public void setStatus(TeacherStatus v){status=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}