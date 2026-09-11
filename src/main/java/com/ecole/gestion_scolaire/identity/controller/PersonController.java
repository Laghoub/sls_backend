package com.ecole.gestion_scolaire.identity.controller;

import com.ecole.gestion_scolaire.identity.dto.PersonCreateRequest;
import com.ecole.gestion_scolaire.identity.dto.PersonResponse;
import com.ecole.gestion_scolaire.identity.dto.PersonUpdateRequest;
import com.ecole.gestion_scolaire.identity.service.PersonService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERSONNE_CONSULTER')")
    public List<PersonResponse> findAll(
            @RequestParam(required = false) String search
    ) {
        return personService.findAll(search);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERSONNE_CONSULTER')")
    public PersonResponse findById(
            @PathVariable Long id
    ) {
        return personService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERSONNE_CREER')")
    public PersonResponse create(
            @Valid @RequestBody PersonCreateRequest request
    ) {
        return personService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERSONNE_MODIFIER')")
    public PersonResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PersonUpdateRequest request
    ) {
        return personService.update(id, request);
    }
}