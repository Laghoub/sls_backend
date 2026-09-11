package com.ecole.gestion_scolaire.student.service;

import com.ecole.gestion_scolaire.identity.entity.Person;
import com.ecole.gestion_scolaire.identity.repository.PersonRepository;
import com.ecole.gestion_scolaire.identity.service.PersonService;
import com.ecole.gestion_scolaire.student.dto.*;
import com.ecole.gestion_scolaire.student.entity.Guardian;
import com.ecole.gestion_scolaire.student.repository.GuardianRepository;
import com.ecole.gestion_scolaire.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class GuardianService {
    private final GuardianRepository repo;
    private final PersonRepository personRepo;

    private final PersonService personService;

    public GuardianService(GuardianRepository r, PersonRepository p , PersonService ps ) {
        repo = r;
        personRepo = p;
        personService = ps;
    }

    public List<GuardianResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public GuardianResponse findById(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public GuardianResponse create(GuardianCreateRequest r) {
        if (repo.existsByPersonId(r.personId()))
            throw new BusinessRuleException("Cette personne possède déjà un profil responsable.");
        Person p = personRepo.findById(r.personId()).orElseThrow(() -> new ResourceNotFoundException("Personne introuvable : " + r.personId()));
        Guardian g = new Guardian();
        g.setPerson(p);
        g.setStatus(norm(r.status()));
        return toResponse(repo.save(g));
    }

    @Transactional
    public GuardianResponse update(Long id, GuardianUpdateRequest r) {
        Guardian g = get(id);
        g.setStatus(norm(r.status()));
        return toResponse(repo.save(g));
    }

    public Guardian get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable : " + id));
    }

    private GuardianResponse toResponse(Guardian g) {
        return new GuardianResponse(g.getId(), g.getPerson().getId(), g.getStatus(), g.getCreatedAt());
    }

    private String norm(String v) {
        return v.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional
    public GuardianWithPersonResponse createWithPerson(
            GuardianWithPersonCreateRequest request
    ) {

        Person person =
                personService.createEntity(
                        request.person()
                );

        Guardian guardian =
                new Guardian();

        guardian.setPerson(person);

        guardian.setStatus(
                request.status()
                        .trim()
                        .toUpperCase()
        );



        Guardian saved =
                repo.save(
                        guardian
                );

        return new GuardianWithPersonResponse(
                toResponse(saved),
                personService.toResponse(person)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<GuardianResponse> search(
            String search,
            int page,
            int size
    ) {

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        100
                );


        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );


        String normalizedSearch =
                search == null
                        ? ""
                        : search.trim();


        Page<GuardianResponse> result =
                repo.search(
                        normalizedSearch,
                        pageable
                ).map(this::toResponse);


        return PageResponse.from(result);
    }
}
