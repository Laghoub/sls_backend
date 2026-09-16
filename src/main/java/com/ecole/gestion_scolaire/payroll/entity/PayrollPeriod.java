package com.ecole.gestion_scolaire.payroll.entity;

import com.ecole.gestion_scolaire.school.entity.SchoolYear;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="payroll_period", uniqueConstraints=@UniqueConstraint(name="uq_payroll_period_year_month",columnNames={"year","month"}))
public class PayrollPeriod {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="school_year_id") private SchoolYear schoolYear;
    @Column(nullable=false) private Integer year;
    @Column(nullable=false) private Integer month;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date",nullable=false) private LocalDate endDate;
    @Column(nullable=false,length=30) private String status;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="closed_at") private OffsetDateTime closedAt;
    @Column(name="closed_by") private Long closedBy;
    @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now();if(status==null)status="OPEN";}
    public Long getId(){return id;} public SchoolYear getSchoolYear(){return schoolYear;} public void setSchoolYear(SchoolYear v){schoolYear=v;}
    public Integer getYear(){return year;} public void setYear(Integer v){year=v;} public Integer getMonth(){return month;} public void setMonth(Integer v){month=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
    public OffsetDateTime getClosedAt(){return closedAt;} public void setClosedAt(OffsetDateTime v){closedAt=v;} public Long getClosedBy(){return closedBy;} public void setClosedBy(Long v){closedBy=v;}
}
