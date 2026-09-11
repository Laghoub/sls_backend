package com.ecole.gestion_scolaire.student.entity;

import com.ecole.gestion_scolaire.identity.entity.Person;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "student", uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_person_id", columnNames = "person_id"),
        @UniqueConstraint(name = "uq_student_student_number", columnNames = "student_number")
})
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;
    @Column(name = "student_number", nullable = false, length = 50)
    private String studentNumber;
    @Column(name = "initial_admission_date")
    private LocalDate initialAdmissionDate;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String v) {
        this.studentNumber = v;
    }

    public LocalDate getInitialAdmissionDate() {
        return initialAdmissionDate;
    }

    public void setInitialAdmissionDate(LocalDate v) {
        this.initialAdmissionDate = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
