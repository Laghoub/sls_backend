package com.ecole.gestion_scolaire.hr.service;

import com.ecole.gestion_scolaire.hr.dto.*;
import com.ecole.gestion_scolaire.accountactivation.dto.ActivationDtos;
import com.ecole.gestion_scolaire.accountactivation.service.AccountActivationService;
import com.ecole.gestion_scolaire.hr.entity.*;
import com.ecole.gestion_scolaire.hr.repository.*;
import com.ecole.gestion_scolaire.identity.repository.*;
import com.ecole.gestion_scolaire.school.repository.*;
import com.ecole.gestion_scolaire.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ecole.gestion_scolaire.identity.entity.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class EmployeeService {
    private final EmployeeRepository repo;
    private final PersonRepository persons;
    private final CampusRepository campuses;
    private final UserAccountRepository users;
    private final EmployeeNumberGenerator numbers;
    private final JobPositionRepository positions;
    private final TeacherRepository teachers;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationService accountActivationService;

    public EmployeeService(EmployeeRepository repo,
                           PersonRepository persons,
                           CampusRepository campuses,
                           UserAccountRepository users,
                           EmployeeNumberGenerator numbers,
                           JobPositionRepository positions,
                           TeacherRepository teachers,
                           RoleRepository roles,
                           UserRoleRepository userRoles,
                           PasswordEncoder passwordEncoder,
                           AccountActivationService accountActivationService) {
        this.repo = repo;
        this.persons = persons;
        this.campuses = campuses;
        this.users = users;
        this.numbers = numbers;
        this.positions = positions;
        this.teachers = teachers;
        this.roles = roles;
        this.userRoles = userRoles;
        this.passwordEncoder = passwordEncoder;
        this.accountActivationService = accountActivationService;
    }

    public EmployeeResponse create(EmployeeRequest q) {
        var e = new Employee();
        e.setPerson(persons.findById(q.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable")));
        e.setEmployeeNumber(numbers.next());
        apply(e, q);
        return map(repo.save(e));
    }

    public EmployeeResponse update(Long id, EmployeeRequest q) {
        var e = get(id);
        if (!e.getPerson().getId().equals(q.personId()))
            throw new BusinessRuleException("La personne liée à un employé ne peut pas être changée.");
        apply(e, q);
        return map(repo.save(e));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> all() {
        return repo.findAllByOrderByPersonLastNameAscPersonFirstNameAsc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse one(Long id) { return map(get(id)); }


    public ActivationDtos.ActivateResponse activateAccount(Long employeeId) {
        var employee = get(employeeId);
        var person = employee.getPerson();
        if (person.getEmail() == null || person.getEmail().isBlank()) {
            throw new BusinessRuleException("Une adresse e-mail est obligatoire avant l'envoi du lien d'activation.");
        }
        String roleCode = resolveAccountRole(employee);
        return accountActivationService.activate(
                new ActivationDtos.ActivateRequest(person.getId(), employee.getEmployeeNumber(), roleCode)
        );
    }

    /** Utilise le code stable du référentiel des postes, jamais son libellé. */
    private String resolveAccountRole(Employee employee) {
        if (teachers.findByEmployeeId(employee.getId()).isPresent()) return "ENSEIGNANT";
        String code = employee.getPosition() == null ? null : employee.getPosition().getCode();
        if (code != null) {
            switch (code.trim().toUpperCase(Locale.ROOT)) {
                case "SURVEILLANT_GENERAL": return "SURVEILLANT_GENERAL";
                case "SURVEILLANT_PRIMAIRE": return "SURVEILLANT_PRIMAIRE";
                case "SURVEILLANT_COLLEGE": return "SURVEILLANT_COLLEGE";
                case "SURVEILLANT_LYCEE": return "SURVEILLANT_LYCEE";
                default: break;
            }
        }
        return "STAFF_ADMINISTRATIF";
    }

    private String uniqueUsername(String employeeNumber) {
        String base = employeeNumber == null || employeeNumber.isBlank() ? "EMP" : employeeNumber.trim().toUpperCase(Locale.ROOT);
        String candidate = base;
        int suffix = 2;
        while (users.existsByUsername(candidate)) candidate = base + "-" + suffix++;
        return candidate;
    }

    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String symbols = "@#$%";
        java.security.SecureRandom random = new java.security.SecureRandom();
        String all = upper + lower + digits + symbols;
        List<Character> chars = new ArrayList<>();
        chars.add(upper.charAt(random.nextInt(upper.length())));
        chars.add(lower.charAt(random.nextInt(lower.length())));
        chars.add(digits.charAt(random.nextInt(digits.length())));
        chars.add(symbols.charAt(random.nextInt(symbols.length())));
        for (int i = 4; i < 12; i++) chars.add(all.charAt(random.nextInt(all.length())));
        Collections.shuffle(chars, random);
        StringBuilder out = new StringBuilder();
        chars.forEach(out::append);
        return out.toString();
    }

    public void changeStatus(Long id, com.ecole.gestion_scolaire.hr.enums.EmploymentStatus status) {
        var e = get(id);
        e.setEmploymentStatus(status);
        repo.save(e);
    }

    Employee get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employé introuvable"));
    }

    private void apply(Employee e, EmployeeRequest q) {
        e.setSocialSecurityNumber(q.socialSecurityNumber());

        JobPosition position = resolvePosition(q);
        e.setPosition(position);
        // Snapshot conservé pour rétrocompatibilité et lecture historique.
        e.setPositionTitle(position.getName());

        e.setCampus(q.campusId() == null ? null : campuses.findById(q.campusId())
                .orElseThrow(() -> new ResourceNotFoundException("Campus introuvable")));
        e.setHireDate(q.hireDate());
        e.setEndDate(q.endDate());
        e.setMaritalStatus(q.maritalStatus());
        if (q.employmentStatus() != null) e.setEmploymentStatus(q.employmentStatus());
    }

    private JobPosition resolvePosition(EmployeeRequest q) {
        JobPosition position;
        if (q.positionId() != null) {
            position = positions.findById(q.positionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable"));
        } else if (q.positionTitle() != null && !q.positionTitle().isBlank()) {
            // Compatibilité temporaire avec le front V15 : on accepte l'intitulé uniquement
            // s'il correspond déjà au référentiel. Aucun texte libre n'est créé ici.
            position = positions.findByNameIgnoreCase(q.positionTitle().trim())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Le poste doit être sélectionné dans le référentiel des postes."));
        } else {
            throw new BusinessRuleException("Le poste de l'employé est obligatoire.");
        }

        if (!position.isActive())
            throw new BusinessRuleException("Ce poste est désactivé et ne peut plus être affecté.");
        return position;
    }

    private EmployeeResponse map(Employee e) {
        var p = e.getPerson();
        var position = e.getPosition();
        return new EmployeeResponse(
                e.getId(), p.getId(), e.getEmployeeNumber(), p.getFirstName(), p.getLastName(),
                p.getEmail(), p.getPhone(),
                position == null ? null : position.getId(),
                position == null ? null : position.getCode(),
                position == null ? e.getPositionTitle() : position.getName(),
                e.getCampus() == null ? null : e.getCampus().getId(),
                e.getCampus() == null ? null : e.getCampus().getName(),
                e.getHireDate(), e.getEndDate(), e.getEmploymentStatus(),
                users.findByPersonId(p.getId()).isPresent()
        );
    }
}
