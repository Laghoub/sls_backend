package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.entity.StudentCharge;
import com.ecole.gestion_scolaire.finance.entity.Tariff;
import com.ecole.gestion_scolaire.finance.enums.ChargeStatus;
import com.ecole.gestion_scolaire.finance.exception.FinanceBusinessException;
import com.ecole.gestion_scolaire.finance.exception.FinanceNotFoundException;
import com.ecole.gestion_scolaire.finance.repository.FeeTypeRepository;
import com.ecole.gestion_scolaire.finance.repository.StudentChargeRepository;
import com.ecole.gestion_scolaire.finance.repository.TariffRepository;
import com.ecole.gestion_scolaire.registration.entity.RegistrationCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

@Service
public class RegistrationChargeService {

    private static final String REGISTRATION_FEE_CODE = "INSCRIPTION";

    private final FeeTypeRepository feeTypes;
    private final TariffRepository tariffs;
    private final StudentChargeRepository charges;

    public RegistrationChargeService(
            FeeTypeRepository feeTypes,
            TariffRepository tariffs,
            StudentChargeRepository charges
    ) {
        this.feeTypes = feeTypes;
        this.tariffs = tariffs;
        this.charges = charges;
    }

    /**
     * Génère la créance correspondant aux frais d'inscription.
     *
     * Cette méthode est appelée lors du passage :
     *
     * PRE_INSCRIPTION -> EN_ATTENTE_PAIEMENT
     *
     * Elle est idempotente :
     * une créance d'inscription non annulée existante n'est jamais recréée.
     */
    @Transactional
    public void createRegistrationCharge(RegistrationCase registration) {

        if (registration == null || registration.getId() == null) {
            throw new FinanceBusinessException(
                    "Le dossier d'inscription est obligatoire."
            );
        }

        var feeType = feeTypes.findByCodeIgnoreCase(REGISTRATION_FEE_CODE)
                .orElseThrow(() -> new FinanceNotFoundException(
                        "Le type de frais INSCRIPTION est introuvable."
                ));

        if (!feeType.isActive()) {
            throw new FinanceBusinessException(
                    "Le type de frais INSCRIPTION est inactif."
            );
        }

        /*
         * Protection contre les doubles clics / retry HTTP.
         */
        boolean alreadyExists =
                charges.existsByRegistrationCaseIdAndFeeTypeIdAndStatusNot(
                        registration.getId(),
                        feeType.getId(),
                        ChargeStatus.CANCELLED.name()
                );

        if (alreadyExists) {
            return;
        }

        LocalDate referenceDate =
                registration.getRegistrationDate() != null
                        ? registration.getRegistrationDate()
                        : LocalDate.now();

        var candidates =
                tariffs.findBySchoolYearIdAndFeeTypeIdAndActiveTrue(
                        registration.getSchoolYearId(),
                        feeType.getId()
                );

        Tariff tariff = candidates.stream()

                /*
                 * Tarif valide à la date du dossier.
                 */
                .filter(t ->
                        !referenceDate.isBefore(t.getValidFrom())
                                &&
                                (
                                        t.getValidUntil() == null
                                                ||
                                                !referenceDate.isAfter(t.getValidUntil())
                                )
                )

                /*
                 * Un tarif ciblant une autre classe est exclu.
                 */
                .filter(t ->
                        t.getClassGroupId() == null
                                ||
                                t.getClassGroupId().equals(
                                        registration.getRequestedClassGroupId()
                                )
                )

                /*
                 * Un tarif ciblant un autre niveau est exclu.
                 */
                .filter(t ->
                        t.getLevelId() == null
                                ||
                                t.getLevelId().equals(
                                        registration.getRequestedLevelId()
                                )
                )

                /*
                 * Tant que RegistrationCase ne contient pas directement
                 * le campus, un tarif uniquement scoped par campus ne peut
                 * pas être résolu de manière fiable ici.
                 *
                 * Un tarif de classe peut en revanche être accepté :
                 * la classe identifie déjà précisément la cible.
                 */
                .filter(t ->
                        t.getCampusId() == null
                                ||
                                (
                                        t.getClassGroupId() != null
                                                &&
                                                t.getClassGroupId().equals(
                                                        registration.getRequestedClassGroupId()
                                                )
                                )
                )

                /*
                 * Priorité :
                 *
                 * 1. classe
                 * 2. niveau
                 * 3. tarif général
                 *
                 * Puis le tarif dont validFrom est le plus récent.
                 */
                .max(
                        Comparator
                                .comparingInt(this::specificity)
                                .thenComparing(Tariff::getValidFrom)
                )

                .orElseThrow(() -> new FinanceBusinessException(
                        "Aucun tarif d'inscription applicable au dossier "
                                + registration.getId()
                                + " pour l'année scolaire sélectionnée."
                ));

        if (tariff.getAmount() == null
                || tariff.getAmount().compareTo(BigDecimal.ZERO) < 0) {

            throw new FinanceBusinessException(
                    "Le montant du tarif d'inscription est invalide."
            );
        }

        var charge = new StudentCharge();

        charge.setRegistrationCaseId(registration.getId());
        charge.setStudentEnrollmentId(null);

        charge.setFeeTypeId(feeType.getId());
        charge.setTariffId(tariff.getId());

        charge.setLabel("Frais d'inscription");

        charge.setOriginalAmount(tariff.getAmount());
        charge.setDiscountAmount(BigDecimal.ZERO);
        charge.setFinalAmount(tariff.getAmount());

        /*
         * Pour les frais d'inscription, l'échéance correspond
         * actuellement à la date d'inscription.
         */
        charge.setDueDate(referenceDate);

        charge.setBillingPeriodStart(null);
        charge.setBillingPeriodEnd(null);

        charge.setStatus(ChargeStatus.DUE.name());

        charges.save(charge);
    }

    private int specificity(Tariff tariff) {

        if (tariff.getClassGroupId() != null) {
            return 3;
        }

        if (tariff.getLevelId() != null) {
            return 2;
        }

        return 1;
    }
}