package com.ecole.gestion_scolaire.student.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="student_family_info", uniqueConstraints=@UniqueConstraint(name="uq_student_family_info_student_id", columnNames="student_id"))
public class StudentFamilyInfo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
    @Column(name="father_life_status",length=20) private String fatherLifeStatus;
    @Column(name="mother_life_status",length=20) private String motherLifeStatus;
    @Column(name="parents_divorced") private Boolean parentsDivorced;
    @Column(name="number_of_brothers") private Integer numberOfBrothers;
    @Column(name="number_of_sisters") private Integer numberOfSisters;
    @Column(columnDefinition="TEXT") private String notes;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @PrePersist @PreUpdate void touch(){updatedAt=OffsetDateTime.now();}
    public Long getId(){return id;} public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public String getFatherLifeStatus(){return fatherLifeStatus;} public void setFatherLifeStatus(String v){fatherLifeStatus=v;}
    public String getMotherLifeStatus(){return motherLifeStatus;} public void setMotherLifeStatus(String v){motherLifeStatus=v;}
    public Boolean getParentsDivorced(){return parentsDivorced;} public void setParentsDivorced(Boolean v){parentsDivorced=v;}
    public Integer getNumberOfBrothers(){return numberOfBrothers;} public void setNumberOfBrothers(Integer v){numberOfBrothers=v;}
    public Integer getNumberOfSisters(){return numberOfSisters;} public void setNumberOfSisters(Integer v){numberOfSisters=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
