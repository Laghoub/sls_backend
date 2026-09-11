package com.ecole.gestion_scolaire.identity.repository;

import com.ecole.gestion_scolaire.identity.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

    @Query("""
            SELECT p
            FROM Person p
            WHERE LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.phone, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY p.lastName, p.firstName
            """)
    List<Person> search(@Param("search") String search);
}