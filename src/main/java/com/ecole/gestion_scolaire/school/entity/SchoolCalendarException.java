package com.ecole.gestion_scolaire.school.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "school_calendar_exception")
public class SchoolCalendarException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "school_year_id",
            nullable = false
    )
    private SchoolYear schoolYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id")
    private Cycle cycle;

    @Column(
            name = "exception_date",
            nullable = false
    )
    private LocalDate exceptionDate;

    @Column(
            name = "exception_type",
            nullable = false,
            length = 40
    )
    private String exceptionType;

    @Column(
            name = "label",
            nullable = false,
            length = 200
    )
    private String label;

    @Column(name = "description")
    private String description;

    public SchoolCalendarException() {
    }

    public Long getId() {
        return id;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(
            SchoolYear schoolYear
    ) {
        this.schoolYear = schoolYear;
    }

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(
            Campus campus
    ) {
        this.campus = campus;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public void setCycle(
            Cycle cycle
    ) {
        this.cycle = cycle;
    }

    public LocalDate getExceptionDate() {
        return exceptionDate;
    }

    public void setExceptionDate(
            LocalDate exceptionDate
    ) {
        this.exceptionDate = exceptionDate;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(
            String exceptionType
    ) {
        this.exceptionType = exceptionType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(
            String label
    ) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description
    ) {
        this.description = description;
    }
}