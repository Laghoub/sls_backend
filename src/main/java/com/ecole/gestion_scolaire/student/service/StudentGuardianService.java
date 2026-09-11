package com.ecole.gestion_scolaire.student.service;

import com.ecole.gestion_scolaire.student.dto.StudentGuardianRequest;
import com.ecole.gestion_scolaire.student.dto.StudentGuardianResponse;
import com.ecole.gestion_scolaire.student.entity.Guardian;
import com.ecole.gestion_scolaire.student.entity.Student;
import com.ecole.gestion_scolaire.student.entity.StudentGuardian;
import com.ecole.gestion_scolaire.student.repository.GuardianRepository;
import com.ecole.gestion_scolaire.student.repository.StudentGuardianRepository;
import com.ecole.gestion_scolaire.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentGuardianService {

 private final StudentGuardianRepository repo;
 private final StudentRepository studentRepository;
 private final GuardianRepository guardianRepository;

 public StudentGuardianService(
         StudentGuardianRepository repo,
         StudentRepository studentRepository,
         GuardianRepository guardianRepository
 ) {
  this.repo = repo;
  this.studentRepository = studentRepository;
  this.guardianRepository = guardianRepository;
 }


 /*
  * =========================================================
  * LISTE DES RESPONSABLES D'UN ÉLÈVE
  * =========================================================
  */

 @Transactional(readOnly = true)
 public List<StudentGuardianResponse> findByStudent(
         Long studentId
 ) {

  /*
   * Vérifie d'abord que l'élève existe.
   */
  getStudent(studentId);

  return repo
          .findByStudentIdOrderByIdAsc(studentId)
          .stream()
          .map(this::toResponse)
          .toList();
 }


 /*
  * =========================================================
  * CRÉATION DU LIEN
  * =========================================================
  */

 public StudentGuardianResponse create(
         Long studentId,
         StudentGuardianRequest request
 ) {

  Student student =
          getStudent(studentId);

  Guardian guardian =
          getGuardian(request.guardianId());


  /*
   * La base possède déjà une contrainte unique :
   *
   * (student_id, guardian_id)
   *
   * On contrôle donc aussi côté service afin de retourner
   * une erreur métier plus compréhensible.
   */
  var existing =
          repo.findByStudentIdAndGuardianId(
                  studentId,
                  request.guardianId()
          );

  if (existing.isPresent()) {

   /*
    * Si le lien existe mais qu'il est inactif,
    * on le réactive au lieu d'essayer d'insérer
    * une nouvelle ligne.
    */
   StudentGuardian link =
           existing.get();

   if (!link.isActive()) {

    apply(
            link,
            request
    );

    link.setActive(true);

    return toResponse(
            repo.save(link)
    );
   }

   throw new IllegalStateException(
           "Ce responsable est déjà lié à cet élève."
   );
  }


  StudentGuardian link =
          new StudentGuardian();

  link.setStudent(student);
  link.setGuardian(guardian);

  apply(
          link,
          request
  );

  return toResponse(
          repo.save(link)
  );
 }


 /*
  * =========================================================
  * MODIFICATION DU LIEN
  * =========================================================
  */

 public StudentGuardianResponse update(
         Long id,
         StudentGuardianRequest request
 ) {

  StudentGuardian link =
          get(id);

  /*
   * Le guardian du lien ne doit normalement pas être
   * remplacé silencieusement.
   *
   * Si le DTO contient un autre guardianId, on refuse
   * l'opération pour préserver la cohérence historique.
   */
  if (
          request.guardianId() != null
                  && !request.guardianId()
                  .equals(
                          link.getGuardian().getId()
                  )
  ) {

   throw new IllegalArgumentException(
           "Le responsable d'un lien existant ne peut pas être remplacé."
   );
  }

  apply(
          link,
          request
  );

  return toResponse(
          repo.save(link)
  );
 }


 /*
  * =========================================================
  * RECHERCHE INTERNE
  * =========================================================
  */

 @Transactional(readOnly = true)
 public StudentGuardian get(Long id) {

  return repo.findById(id)
          .orElseThrow(
                  () ->
                          new IllegalArgumentException(
                                  "Lien élève/responsable introuvable : "
                                          + id
                          )
          );
 }


 private Student getStudent(
         Long studentId
 ) {

  return studentRepository
          .findById(studentId)
          .orElseThrow(
                  () ->
                          new IllegalArgumentException(
                                  "Élève introuvable : "
                                          + studentId
                          )
          );
 }


 private Guardian getGuardian(
         Long guardianId
 ) {

  if (guardianId == null) {

   throw new IllegalArgumentException(
           "Le responsable est obligatoire."
   );
  }

  return guardianRepository
          .findById(guardianId)
          .orElseThrow(
                  () ->
                          new IllegalArgumentException(
                                  "Responsable introuvable : "
                                          + guardianId
                          )
          );
 }


 /*
  * =========================================================
  * APPLICATION DU DTO SUR L'ENTITÉ
  * =========================================================
  */

 private void apply(
         StudentGuardian link,
         StudentGuardianRequest request
 ) {

  link.setRelationshipType(
          normalize(
                  request.relationshipType()
          )
  );

  link.setLegalGuardian(
          request.legalGuardian()
  );

  link.setFinancialResponsible(
          request.financialResponsible()
  );

  link.setPrimaryContact(
          request.primaryContact()
  );

  link.setLivesWithStudent(
          request.livesWithStudent()
  );

  link.setPickupAuthorized(
          request.pickupAuthorized()
  );

  link.setActive(
          request.active()
  );

  link.setValidFrom(
          request.validFrom()
  );

  link.setValidUntil(
          request.validUntil()
  );
 }


 /*
  * =========================================================
  * ENTITY -> RESPONSE
  * =========================================================
  */

 private StudentGuardianResponse toResponse(
         StudentGuardian link
 ) {

  Guardian guardian =
          link.getGuardian();

  var person =
          guardian.getPerson();

  return new StudentGuardianResponse(

          link.getId(),

          link.getStudent().getId(),

          guardian.getId(),

          /*
           * Données de Person.
           */
          person.getLastName(),

          person.getFirstName(),

          person.getPhone(),

          person.getEmail(),

          link.getRelationshipType(),

          link.isLegalGuardian(),

          link.isFinancialResponsible(),

          link.isPrimaryContact(),

          link.getLivesWithStudent(),

          link.isPickupAuthorized(),

          link.isActive(),

          link.getValidFrom(),

          link.getValidUntil()
  );
 }


 /*
  * =========================================================
  * NORMALISATION
  * =========================================================
  */

 private String normalize(
         String value
 ) {

  if (value == null) {
   return null;
  }

  String normalized =
          value.trim()
                  .toUpperCase();

  return normalized.isBlank()
          ? null
          : normalized;
 }
}