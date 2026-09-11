package com.ecole.gestion_scolaire.student.entity;

import com.ecole.gestion_scolaire.identity.entity.Person;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="guardian", uniqueConstraints=@UniqueConstraint(name="uq_guardian_person_id", columnNames="person_id"))
public class Guardian {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="person_id", nullable=false) private Person person;
    @Column(nullable=false,length=30) private String status;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @PrePersist void prePersist(){if(createdAt==null) createdAt=OffsetDateTime.now();}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Person getPerson(){return person;} public void setPerson(Person v){this.person=v;}
    public String getStatus(){return status;} public void setStatus(String v){this.status=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
}
