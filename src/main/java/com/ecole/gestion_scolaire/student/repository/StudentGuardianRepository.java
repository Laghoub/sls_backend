package com.ecole.gestion_scolaire.student.repository;

import com.ecole.gestion_scolaire.student.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGuardianRepository
        extends JpaRepository<StudentGuardian, Long> {

    /*
     * Liste des responsables liés à un élève.
     */
    List<StudentGuardian> findByStudentIdOrderByIdAsc(
            Long studentId
    );


    /*
     * Recherche d'un lien précis.
     */
    Optional<StudentGuardian> findByStudentIdAndGuardianId(
            Long studentId,
            Long guardianId
    );


    /*
     * Vérifie que le responsable principal du dossier
     * est lié activement à l'élève.
     */
    boolean existsByStudentIdAndGuardianIdAndActiveTrue(
            Long studentId,
            Long guardianId
    );


    /*
     * Vérifie qu'au moins un responsable légal actif existe.
     */
    boolean existsByStudentIdAndActiveTrueAndLegalGuardianTrue(
            Long studentId
    );

    List<StudentGuardian> findByGuardianIdAndActiveTrueOrderByIdAsc(Long guardianId);
}