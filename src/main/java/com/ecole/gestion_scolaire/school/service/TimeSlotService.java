package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.TimeSlotCreateRequest;
import com.ecole.gestion_scolaire.school.dto.TimeSlotResponse;
import com.ecole.gestion_scolaire.school.dto.TimeSlotUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.TimeSlot;
import com.ecole.gestion_scolaire.school.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlotService(
            TimeSlotRepository timeSlotRepository
    ) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<TimeSlotResponse> findAll() {

        return timeSlotRepository
                .findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TimeSlotResponse> findActive() {

        return timeSlotRepository
                .findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TimeSlotResponse findById(
            Long id
    ) {

        return toResponse(
                getEntityById(id)
        );
    }

    @Transactional
    public TimeSlotResponse create(
            TimeSlotCreateRequest request
    ) {

        validateTimes(
                request.startTime(),
                request.endTime()
        );

        String code =
                normalizeCode(request.code());

        validateCodeAvailable(
                code,
                null
        );

        TimeSlot timeSlot = new TimeSlot();

        timeSlot.setCode(code);
        timeSlot.setStartTime(request.startTime());
        timeSlot.setEndTime(request.endTime());
        timeSlot.setDisplayOrder(request.displayOrder());
        timeSlot.setActive(request.active());

        return toResponse(
                timeSlotRepository.save(timeSlot)
        );
    }

    @Transactional
    public TimeSlotResponse update(
            Long id,
            TimeSlotUpdateRequest request
    ) {

        TimeSlot timeSlot =
                getEntityById(id);

        validateTimes(
                request.startTime(),
                request.endTime()
        );

        String code =
                normalizeCode(request.code());

        validateCodeAvailable(
                code,
                id
        );

        timeSlot.setCode(code);
        timeSlot.setStartTime(request.startTime());
        timeSlot.setEndTime(request.endTime());
        timeSlot.setDisplayOrder(request.displayOrder());
        timeSlot.setActive(request.active());

        return toResponse(
                timeSlotRepository.save(timeSlot)
        );
    }

    private TimeSlot getEntityById(
            Long id
    ) {

        return timeSlotRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Créneau horaire introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private void validateTimes(
            LocalTime startTime,
            LocalTime endTime
    ) {

        if (!endTime.isAfter(startTime)) {

            throw new BusinessRuleException(
                    "L'heure de fin doit être postérieure à l'heure de début."
            );
        }
    }

    private void validateCodeAvailable(
            String code,
            Long currentId
    ) {

        timeSlotRepository
                .findByCode(code)
                .ifPresent(existing -> {

                    if (
                            currentId == null
                                    || !existing.getId().equals(currentId)
                    ) {

                        throw new BusinessRuleException(
                                "Un créneau horaire avec le code '"
                                        + code
                                        + "' existe déjà."
                        );
                    }
                });
    }

    private String normalizeCode(
            String value
    ) {

        return value
                .trim()
                .toUpperCase();
    }

    private TimeSlotResponse toResponse(
            TimeSlot timeSlot
    ) {

        return new TimeSlotResponse(
                timeSlot.getId(),
                timeSlot.getCode(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                timeSlot.getDisplayOrder(),
                timeSlot.isActive()
        );
    }
}