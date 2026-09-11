package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cash_register")
public class CashRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "campus_id")
    private Long campusId;
    @Column(nullable = false)
    private boolean active;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String v) {
        code = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public Long getCampusId() {
        return campusId;
    }

    public void setCampusId(Long v) {
        campusId = v;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        active = v;
    }
}
