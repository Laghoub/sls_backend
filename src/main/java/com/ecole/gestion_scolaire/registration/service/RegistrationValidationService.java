package com.ecole.gestion_scolaire.registration.service;

import com.ecole.gestion_scolaire.registration.dto.RegistrationValidationResponse;
import com.ecole.gestion_scolaire.registration.repository.RegistrationConsentRepository;
import com.ecole.gestion_scolaire.registration.repository.RegistrationDocumentRequirementRepository;
import com.ecole.gestion_scolaire.registration.repository.RegistrationDocumentStatusRepository;
import com.ecole.gestion_scolaire.student.repository.StudentFamilyInfoRepository;
import com.ecole.gestion_scolaire.student.repository.StudentGuardianRepository;
import com.ecole.gestion_scolaire.student.repository.StudentMedicalInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistrationValidationService {

 private final RegistrationCaseService cases;

 private final RegistrationDocumentRequirementRepository requirements;

 private final RegistrationDocumentStatusRepository documentStatuses;

 private final RegistrationConsentRepository consents;

 private final StudentGuardianRepository studentGuardianRepository;

 private final StudentFamilyInfoRepository familyInfoRepository;

 private final StudentMedicalInfoRepository medicalInfoRepository;


 public RegistrationValidationService(
         RegistrationCaseService cases,
         RegistrationDocumentRequirementRepository requirements,
         RegistrationDocumentStatusRepository documentStatuses,
         RegistrationConsentRepository consents,
         StudentGuardianRepository studentGuardianRepository,
         StudentFamilyInfoRepository familyInfoRepository,
         StudentMedicalInfoRepository medicalInfoRepository
 ) {

  this.cases = cases;
  this.requirements = requirements;
  this.documentStatuses = documentStatuses;
  this.consents = consents;
  this.studentGuardianRepository = studentGuardianRepository;
  this.familyInfoRepository = familyInfoRepository;
  this.medicalInfoRepository = medicalInfoRepository;
 }


 /**
  * Vérifie si le dossier d'inscription possède toutes les
  * informations nécessaires pour être finalisé.
  */
 public RegistrationValidationResponse validate(Long registrationId) {

  var registration = cases.get(registrationId);

  Long studentId = registration.getStudent().getId();
  Long guardianId = registration.getGuardian().getId();

  List<String> missing = new ArrayList<>();


  /*
   * =====================================================
   * 1. RESPONSABLE PRINCIPAL LIÉ À L'ÉLÈVE
   * =====================================================
   *
   * Le responsable sélectionné dans le dossier
   * d'inscription doit réellement être lié à l'élève
   * dans student_guardian.
   */

  boolean principalGuardianLinked =
          studentGuardianRepository
                  .existsByStudentIdAndGuardianIdAndActiveTrue(
                          studentId,
                          guardianId
                  );

  if (!principalGuardianLinked) {

   missing.add(
           "Le responsable principal doit être lié à l’élève."
   );
  }


  /*
   * =====================================================
   * 2. AU MOINS UN RESPONSABLE LÉGAL ACTIF
   * =====================================================
   */

  boolean hasLegalGuardian =
          studentGuardianRepository
                  .existsByStudentIdAndActiveTrueAndLegalGuardianTrue(
                          studentId
                  );

  if (!hasLegalGuardian) {

   missing.add(
           "Au moins un responsable légal actif doit être renseigné."
   );
  }


  /*
   * =====================================================
   * 3. INFORMATIONS FAMILIALES
   * =====================================================
   *
   * On exige que la rubrique ait été vérifiée et
   * enregistrée.
   *
   * On ne rend pas chaque champ obligatoire :
   * certaines informations peuvent être inconnues
   * ou non applicables.
   */

  boolean familyInfoExists =
          familyInfoRepository
                  .findByStudentId(studentId)
                  .isPresent();

  if (!familyInfoExists) {

   missing.add(
           "Les informations familiales doivent être vérifiées et enregistrées."
   );
  }


  /*
   * =====================================================
   * 4. INFORMATIONS MÉDICALES
   * =====================================================
   *
   * L'élève n'est évidemment pas obligé d'avoir une
   * maladie, une allergie ou un traitement.
   *
   * On exige uniquement qu'une fiche médicale existe,
   * ce qui signifie que la rubrique a été vérifiée.
   */

  boolean medicalInfoExists =
          medicalInfoRepository
                  .findByStudentId(studentId)
                  .isPresent();

  if (!medicalInfoExists) {

   missing.add(
           "La rubrique médicale doit être vérifiée et enregistrée."
   );
  }


  /*
   * =====================================================
   * 5. PIÈCES OBLIGATOIRES
   * =====================================================
   *
   * Chaque pièce configurée comme :
   *
   * active = true
   * required = true
   *
   * doit être fournie ET vérifiée.
   */

  var requiredDocuments =
          requirements
                  .findByActiveTrueAndRequiredTrueOrderByNameAsc();

  for (var requirement : requiredDocuments) {

   var status =
           documentStatuses
                   .findByRegistrationCaseIdAndRequirementId(
                           registrationId,
                           requirement.getId()
                   );

   if (status.isEmpty()) {

    missing.add(
            "Pièce obligatoire manquante : "
                    + requirement.getName()
    );

    continue;
   }

   if (!status.get().isProvided()) {

    missing.add(
            "Pièce obligatoire non fournie : "
                    + requirement.getName()
    );

    continue;
   }

   if (!status.get().isVerified()) {

    missing.add(
            "Pièce obligatoire non vérifiée : "
                    + requirement.getName()
    );
   }
  }


  /*
   * =====================================================
   * 6. CONSENTEMENTS
   * =====================================================
   *
   * Les consentements déjà enregistrés dans le dossier
   * doivent être acceptés.
   *
   * Plus tard, si certains types de consentement deviennent
   * obligatoires, nous pourrons également vérifier leur
   * présence explicitement.
   */

  var registrationConsents =
          consents
                  .findByRegistrationCaseIdOrderByConsentTypeAsc(
                          registrationId
                  );

  for (var consent : registrationConsents) {

   if (!consent.isAccepted()) {

    missing.add(
            "Consentement non accepté : "
                    + consent.getConsentType()
    );
   }
  }


  /*
   * =====================================================
   * 7. RÉSULTAT FINAL
   * =====================================================
   */

  boolean valid = missing.isEmpty();

  return new RegistrationValidationResponse(
          valid,
          List.copyOf(missing)
  );
 }
}