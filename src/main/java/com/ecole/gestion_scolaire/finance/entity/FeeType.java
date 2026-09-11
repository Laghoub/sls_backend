package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fee_type")
public class FeeType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "display_order")
    private Integer displayOrder;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String v) {
        category = v;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        active = v;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer v) {
        displayOrder = v;
    }
}
