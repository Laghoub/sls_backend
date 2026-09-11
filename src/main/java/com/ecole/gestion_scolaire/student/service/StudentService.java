package com.ecole.gestion_scolaire.student.service;

import com.ecole.gestion_scolaire.identity.entity.Person;
import com.ecole.gestion_scolaire.identity.repository.PersonRepository;
import com.ecole.gestion_scolaire.identity.service.PersonService;
import com.ecole.gestion_scolaire.student.dto.*;
import com.ecole.gestion_scolaire.student.entity.Student;
import com.ecole.gestion_scolaire.student.repository.StudentRepository;
import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import com.ecole.gestion_scolaire.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository repo;
    private final PersonRepository personRepo;
    private final PersonService personService;

    private final StudentNumberGenerator studentNumberGenerator;

    public StudentService(
            StudentRepository studentRepository,
            PersonRepository personRepository,
            PersonService personService,
            StudentNumberGenerator studentNumberGenerator
    ) {
        this.repo = studentRepository;
        this.personRepo = personRepository;
        this.personService = personService;
        this.studentNumberGenerator =studentNumberGenerator;
    }

    public List<StudentResponse> findAll() {
        return repo.findAllByOrderByStudentNumberAsc().stream().map(this::toResponse).toList();
    }

    public StudentResponse findById(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public StudentResponse create(StudentCreateRequest r) {
        if (repo.existsByPersonId(r.personId()))
            throw new BusinessRuleException("Cette personne possède déjà un profil élève.");
        String number = normalize(r.studentNumber());
        if (repo.existsByStudentNumber(number)) throw new BusinessRuleException("Ce numéro élève existe déjà.");
        Person p = personRepo.findById(r.personId()).orElseThrow(() -> new ResourceNotFoundException("Personne introuvable : " + r.personId()));
        Student s = new Student();
        s.setPerson(p);
        s.setStudentNumber(number);
        s.setInitialAdmissionDate(r.initialAdmissionDate());
        s.setStatus(normalize(r.status()));
        return toResponse(repo.save(s));
    }

    @Transactional
    public StudentResponse update(
            Long id,
            StudentUpdateRequest r
    ) {

        Student s = get(id);

        s.setInitialAdmissionDate(
                r.initialAdmissionDate()
        );

        s.setStatus(
                normalize(r.status())
        );

        return toResponse(
                repo.save(s)
        );
    }

    public Student get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Élève introuvable : " + id));
    }

    private StudentResponse toResponse(Student s) {
        return new StudentResponse(s.getId(), s.getPerson().getId(), s.getStudentNumber(), s.getInitialAdmissionDate(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private String normalize(String v) {
        return v == null ? null : v.trim().toUpperCase(Locale.ROOT);
    }


    @Transactional
    public StudentWithPersonResponse createWithPerson(
            StudentWithPersonCreateRequest request
    ) {

        Person person =
                personService.createEntity(
                        request.person()
                );

        String studentNumber =
                studentNumberGenerator.generate();


        /*
         * La séquence PostgreSQL garantit déjà
         * normalement l'unicité.
         *
         * La contrainte UNIQUE en base reste
         * notre dernière protection.
         */
        if (
                repo.existsByStudentNumber(
                        studentNumber
                )
        ) {

            throw new BusinessRuleException(
                    "Le matricule élève généré existe déjà."
            );
        }


        Student student =
                new Student();

        student.setPerson(person);

        student.setStudentNumber(
                studentNumber
        );

        student.setInitialAdmissionDate(
                request.initialAdmissionDate()
        );

        student.setStatus(
                request.status()
                        .trim()
                        .toUpperCase()
        );


        OffsetDateTime now =
                OffsetDateTime.now();



        Student saved =
                repo.save(student);


        return new StudentWithPersonResponse(
                toResponse(saved),
                personService.toResponse(person)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> search(
            String search,
            int page,
            int size
    ) {

        int safePage = Math.max(page, 0);

        int safeSize = Math.min(
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


        Page<StudentResponse> result =
                repo.search(
                        normalizedSearch,
                        pageable
                ).map(this::toResponse);


        return PageResponse.from(result);
    }


}
