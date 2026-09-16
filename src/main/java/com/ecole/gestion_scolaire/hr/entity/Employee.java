package com.ecole.gestion_scolaire.hr.entity;

import com.ecole.gestion_scolaire.identity.entity.Person;
import com.ecole.gestion_scolaire.school.entity.Campus;
import com.ecole.gestion_scolaire.hr.enums.EmploymentStatus;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "employee_number", nullable = false, length = 50)
    private String employeeNumber;

    @Column(name = "social_security_number", length = 100)
    private String socialSecurityNumber;

    // Conservé pour compatibilité/historique avec les versions précédentes.
    @Column(name = "position_title", nullable = false, length = 150)
    private String positionTitle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private JobPosition position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus;

    @Column(name = "marital_status", length = 30)
    private String maritalStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void pre() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (employmentStatus == null) employmentStatus = EmploymentStatus.ACTIVE;
    }

    @PreUpdate
    void upd() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Person getPerson() { return person; }
    public void setPerson(Person v) { person = v; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String v) { employeeNumber = v; }
    public String getSocialSecurityNumber() { return socialSecurityNumber; }
    public void setSocialSecurityNumber(String v) { socialSecurityNumber = v; }
    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String v) { positionTitle = v; }
    public JobPosition getPosition() { return position; }
    public void setPosition(JobPosition v) { position = v; }
    public Campus getCampus() { return campus; }
    public void setCampus(Campus v) { campus = v; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate v) { hireDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { endDate = v; }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(EmploymentStatus v) { employmentStatus = v; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String v) { maritalStatus = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
