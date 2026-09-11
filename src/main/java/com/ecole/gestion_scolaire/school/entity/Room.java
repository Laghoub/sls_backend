package com.ecole.gestion_scolaire.school.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "room",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_room_campus_id_code",
                        columnNames = {
                                "campus_id",
                                "code"
                        }
                )
        }
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "campus_id",
            nullable = false
    )
    private Campus campus;

    @Column(
            name = "code",
            nullable = false,
            length = 30
    )
    private String code;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "capacity"
    )
    private Integer capacity;

    @Column(
            name = "room_type",
            length = 30
    )
    private String roomType;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

    public Room() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(Campus campus) {
        this.campus = campus;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}