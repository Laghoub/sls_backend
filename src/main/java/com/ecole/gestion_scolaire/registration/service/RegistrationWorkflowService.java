package com.ecole.gestion_scolaire.registration.service;

import com.ecole.gestion_scolaire.finance.service.RegistrationChargeService;
import com.ecole.gestion_scolaire.registration.dto.*;
import com.ecole.gestion_scolaire.registration.enums.RegistrationStatus;
import com.ecole.gestion_scolaire.registration.exception.*;
import com.ecole.gestion_scolaire.registration.repository.RegistrationCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RegistrationWorkflowService {

 private final RegistrationCaseService cases;
 private final RegistrationCaseRepository repo;
 private final RegistrationValidationService validation;
 private final RegistrationDocumentService docs;
 private final StudentEnrollmentService enrollments;

 /*
  * Pont Registration -> Finance.
  */
 private final RegistrationChargeService registrationCharges;

 public RegistrationWorkflowService(
         RegistrationCaseService cases,
         RegistrationCaseRepository repo,
         RegistrationValidationService validation,
         RegistrationDocumentService docs,
         StudentEnrollmentService enrollments,
         RegistrationChargeService registrationCharges
 ) {
  this.cases = cases;
  this.repo = repo;
  this.validation = validation;
  this.docs = docs;
  this.enrollments = enrollments;
  this.registrationCharges = registrationCharges;
 }

 /**
  * Envoie une pré-inscription vers Finance.
  *
  * La créance est créée AVANT le changement de statut.
  *
  * Comme toute la méthode est transactionnelle :
  *
  * - si la création de la créance échoue,
  *   le dossier reste PRE_INSCRIPTION ;
  *
  * - si tout réussit,
  *   la créance et EN_ATTENTE_PAIEMENT sont commit ensemble.
  */
 @Transactional
 public RegistrationCaseResponse submitForPayment(Long id) {

  var registration = cases.get(id);

  require(
          registration.getStatus() == RegistrationStatus.PRE_INSCRIPTION,
          "Seule une pré-inscription peut être envoyée au paiement."
  );

  /*
   * Finance doit accepter le dossier avant que son statut
   * ne soit modifié.
   */
  registrationCharges.createRegistrationCharge(registration);

  registration.setStatus(
          RegistrationStatus.EN_ATTENTE_PAIEMENT
  );

  return cases.toResponse(
          repo.save(registration)
  );
 }

 /**
  * Point d'entrée appelé après validation réelle du paiement.
  *
  * À terme, cette méthode ne doit pas être déclenchée
  * manuellement depuis l'interface d'inscription.
  */
 @Transactional
 public RegistrationCaseResponse paymentConfirmed(Long id) {

  var registration = cases.get(id);

  require(
          registration.getStatus()
                  == RegistrationStatus.EN_ATTENTE_PAIEMENT,
          "Le dossier doit être en attente de paiement."
  );

  registration.setStatus(
          RegistrationStatus.PAYE_A_COMPLETER
  );

  docs.initialize(id);

  return cases.toResponse(
          repo.save(registration)
  );
 }

 @Transactional
 public RegistrationCaseResponse startCompletion(Long id) {

  var registration = cases.get(id);

  require(
          registration.getStatus()
                  == RegistrationStatus.PAYE_A_COMPLETER
                  ||
                  registration.getStatus()
                          == RegistrationStatus.INCOMPLET,
          "Le dossier n’est pas disponible pour complément."
  );

  registration.setStatus(
          RegistrationStatus.EN_COURS
  );

  return cases.toResponse(
          repo.save(registration)
  );
 }

 @Transactional
 public RegistrationCaseResponse markIncomplete(Long id) {

  var registration = cases.get(id);

  require(
          registration.getStatus()
                  == RegistrationStatus.EN_COURS,
          "Seul un dossier en cours peut être marqué incomplet."
  );

  registration.setStatus(
          RegistrationStatus.INCOMPLET
  );

  return cases.toResponse(
          repo.save(registration)
  );
 }

 @Transactional
 public RegistrationCaseDetailResponse finalizeRegistration(
         Long id,
         StudentEnrollmentCreateRequest request
 ) {

  var registration = cases.get(id);

  require(
          registration.getStatus()
                  == RegistrationStatus.EN_COURS
                  ||
                  registration.getStatus()
                          == RegistrationStatus.INCOMPLET,
          "Le dossier doit être en cours de complément."
  );

  var validationResult = validation.validate(id);

  if (!validationResult.valid()) {
   throw new RegistrationValidationException(
           String.join(
                   " | ",
                   validationResult.missingItems()
           )
   );
  }

  if (request.classGroupId() == null) {
   throw new RegistrationValidationException(
           "Une classe est obligatoire pour finaliser la scolarisation."
   );
  }

  var enrollment =
          enrollments.createFromRegistration(
                  registration,
                  request
          );

  registration.setRequestedClassGroupId(
          request.classGroupId()
  );

  registration.setStatus(
          RegistrationStatus.FINALISE
  );

  registration.setFinalizedAt(
          OffsetDateTime.now()
  );

  repo.save(registration);

  return new RegistrationCaseDetailResponse(
          cases.toResponse(registration),
          null,
          null,
          null,
          validationResult,
          enrollment
  );
 }

 @Transactional
 public RegistrationCaseResponse cancel(
         Long id,
         String reason
 ) {

  var registration = cases.get(id);

  require(
          registration.getStatus()
                  != RegistrationStatus.FINALISE
                  &&
                  registration.getStatus()
                          != RegistrationStatus.ANNULE,
          "Ce dossier ne peut plus être annulé."
  );

  registration.setStatus(
          RegistrationStatus.ANNULE
  );

  registration.setCancelledAt(
          OffsetDateTime.now()
  );

  registration.setCancellationReason(
          reason.trim()
  );

  return cases.toResponse(
          repo.save(registration)
  );
 }

 private void require(
         boolean condition,
         String message
 ) {
  if (!condition) {
   throw new InvalidRegistrationTransitionException(
           message
   );
  }
 }
}