package com.ecole.gestion_scolaire.audit.service;

import com.ecole.gestion_scolaire.audit.dto.AuditLogResponse;
import com.ecole.gestion_scolaire.audit.entity.AuditLog;
import com.ecole.gestion_scolaire.audit.repository.AuditLogRepository;
import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.identity.entity.Person;
import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.identity.repository.UserAccountRepository;
import com.ecole.gestion_scolaire.security.service.SecurityUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
public class AuditService {
    private final AuditLogRepository repo;
    private final UserAccountRepository userAccountRepository;

    public AuditService(AuditLogRepository repo, UserAccountRepository userAccountRepository) {
        this.repo = repo;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SecurityUser u, String module, String action, String entityType, String entityId,
                       String description, String oldValues, String newValues, String method, String path,
                       Integer status, String ip, String requestId) {
        AuditLog x = new AuditLog();

        if (u != null) {
            x.setUserAccountId(u.getId());
            x.setUsername(u.getUsername());

            // Recharge le compte avec Person dans la transaction d'audit.
            // Cela évite LazyInitializationException après la fin de la requête métier.
            UserAccount account = userAccountRepository.findOneById(u.getId()).orElse(null);
            if (account != null) {
                Person p = account.getPerson();
                if (p != null) {
                    x.setUserLastName(p.getLastName());
                    x.setUserFirstName(p.getFirstName());
                }
            }

            x.setUserRoles(u.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }

        x.setModule(defaultValue(module, "AUTRE"));
        x.setAction(defaultValue(action, "ACTION"));
        x.setEntityType(defaultValue(entityType, "REQUETE_API"));
        x.setEntityId(entityId);
        x.setDescription(description);
        x.setOldValues(oldValues);
        x.setNewValues(newValues);
        x.setHttpMethod(method);
        x.setRequestPath(path);
        x.setHttpStatus(status);
        x.setIpAddress(ip);
        x.setRequestId(requestId);
        repo.saveAndFlush(x);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String module, String action, Long userId, String q,
                                                  LocalDate from, LocalDate to, int page, int size) {
        Specification<AuditLog> specification = (root, query, cb) -> cb.conjunction();

        if (module != null && !module.isBlank())
            specification = specification.and((r, c, b) -> b.equal(r.get("module"), module));
        if (action != null && !action.isBlank())
            specification = specification.and((r, c, b) -> b.equal(r.get("action"), action));
        if (userId != null)
            specification = specification.and((r, c, b) -> b.equal(r.get("userAccountId"), userId));

        if (from != null) {
            OffsetDateTime start = from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            specification = specification.and((r, c, b) -> b.greaterThanOrEqualTo(r.get("occurredAt"), start));
        }
        if (to != null) {
            OffsetDateTime end = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            specification = specification.and((r, c, b) -> b.lessThan(r.get("occurredAt"), end));
        }

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            specification = specification.and((r, c, b) -> b.or(
                    b.like(b.lower(r.get("username")), like),
                    b.like(b.lower(r.get("userLastName")), like),
                    b.like(b.lower(r.get("userFirstName")), like),
                    b.like(b.lower(r.get("description")), like),
                    b.like(b.lower(r.get("requestPath")), like),
                    b.like(b.lower(r.get("entityId")), like)
            ));
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(200, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return PageResponse.from(repo.findAll(specification, pageable).map(this::dto));
    }

    private AuditLogResponse dto(AuditLog x) {
        return new AuditLogResponse(x.getId(), x.getOccurredAt(), x.getUserAccountId(), x.getUsername(),
                x.getUserLastName(), x.getUserFirstName(), x.getUserRoles(), x.getModule(), x.getAction(),
                x.getEntityType(), x.getEntityId(), x.getDescription(), x.getOldValues(), x.getNewValues(),
                x.getHttpMethod(), x.getRequestPath(), x.getHttpStatus(), x.getIpAddress(), x.getRequestId());
    }
}
