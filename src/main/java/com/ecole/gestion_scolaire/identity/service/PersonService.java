package com.ecole.gestion_scolaire.identity.service;

import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.identity.dto.PersonCreateRequest;
import com.ecole.gestion_scolaire.identity.dto.PersonResponse;
import com.ecole.gestion_scolaire.identity.dto.PersonUpdateRequest;
import com.ecole.gestion_scolaire.identity.entity.Person;
import com.ecole.gestion_scolaire.identity.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<PersonResponse> findAll(String search) {

        List<Person> persons;

        if (search == null || search.isBlank()) {
            persons = personRepository.findAll();
        } else {
            persons = personRepository.search(search.trim());
        }

        return persons.stream()
                .map(this::toResponse)
                .toList();
    }

    public PersonResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public Person findEntityById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Personne introuvable avec l'id : " + id
                        )
                );
    }

    @Transactional
    public Person createEntity(PersonCreateRequest request) {

        Person person = new Person();

        applyCreateRequest(person, request);

        OffsetDateTime now = OffsetDateTime.now();

        person.setCreatedAt(now);
        person.setUpdatedAt(now);

        return personRepository.save(person);
    }

    @Transactional
    public PersonResponse create(PersonCreateRequest request) {
        return toResponse(createEntity(request));
    }

    @Transactional
    public PersonResponse update(
            Long id,
            PersonUpdateRequest request
    ) {

        Person person = findEntityById(id);

        person.setLastName(normalizeRequired(request.lastName()));
        person.setFirstName(normalizeRequired(request.firstName()));
        person.setBirthDate(request.birthDate());
        person.setBirthPlace(normalizeNullable(request.birthPlace()));
        person.setNationality(normalizeNullable(request.nationality()));
        person.setSex(normalizeNullable(request.sex()));
        person.setAddress(normalizeNullable(request.address()));
        person.setPhone(normalizeNullable(request.phone()));
        person.setSecondaryPhone(normalizeNullable(request.secondaryPhone()));
        person.setEmail(normalizeNullableLowercase(request.email()));
        person.setPhotoReference(normalizeNullable(request.photoReference()));

        person.setUpdatedAt(OffsetDateTime.now());

        return toResponse(personRepository.save(person));
    }

    private void applyCreateRequest(
            Person person,
            PersonCreateRequest request
    ) {

        person.setLastName(normalizeRequired(request.lastName()));
        person.setFirstName(normalizeRequired(request.firstName()));
        person.setBirthDate(request.birthDate());
        person.setBirthPlace(normalizeNullable(request.birthPlace()));
        person.setNationality(normalizeNullable(request.nationality()));
        person.setSex(normalizeNullable(request.sex()));
        person.setAddress(normalizeNullable(request.address()));
        person.setPhone(normalizeNullable(request.phone()));
        person.setSecondaryPhone(normalizeNullable(request.secondaryPhone()));
        person.setEmail(normalizeNullableLowercase(request.email()));
        person.setPhotoReference(normalizeNullable(request.photoReference()));
    }

    public PersonResponse toResponse(Person person) {

        return new PersonResponse(
                person.getId(),
                person.getLastName(),
                person.getFirstName(),
                person.getBirthDate(),
                person.getBirthPlace(),
                person.getNationality(),
                person.getSex(),
                person.getAddress(),
                person.getPhone(),
                person.getSecondaryPhone(),
                person.getEmail(),
                person.getPhotoReference(),
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private String normalizeNullableLowercase(String value) {

        String normalized = normalizeNullable(value);

        return normalized == null
                ? null
                : normalized.toLowerCase();
    }
}