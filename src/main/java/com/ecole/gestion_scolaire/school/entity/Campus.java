package com.ecole.gestion_scolaire.school.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "campus",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_campus_code",
                        columnNames = "code"
                )
        }
)
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "code",
            nullable = false,
            length = 30
    )
    private String code;

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            name = "address",
            length = 300
    )
    private String address;

    @Column(
            name = "phone",
            length = 30
    )
    private String phone;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

    public Campus() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}