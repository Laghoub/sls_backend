package com.ecole.gestion_scolaire.guardianmerge;

import com.ecole.gestion_scolaire.student.repository.GuardianRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** Fusion strictement transactionnelle, refusée dès qu'une relation n'est pas prise en charge. */
@Service
public class GuardianMergeService {
    private static final Set<String> TRANSFERABLE = Set.of(
        "student_guardian.guardian_id", "registration_case.guardian_id",
        "payment.guardian_id", "family_credit.guardian_id",
        "registration_consent.accepted_by_guardian_id",
        "attendance_justification.submitted_by_guardian_id",
        "parent_teacher_appointment.guardian_id");
    private final JdbcTemplate jdbc;
    private final GuardianRepository guardians;
    public GuardianMergeService(JdbcTemplate jdbc, GuardianRepository guardians) {
        this.jdbc = jdbc; this.guardians = guardians;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(Long primaryId, Long duplicateId) {
        return inspect(primaryId, duplicateId);
    }

    private Map<String, Object> inspect(Long primaryId, Long duplicateId) {
        validate(primaryId, duplicateId);
        var primary = guardians.findById(primaryId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent principal introuvable"));
        var duplicate = guardians.findById(duplicateId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent doublon introuvable"));
        List<String> blockers = new ArrayList<>();
        if ("MERGED".equalsIgnoreCase(primary.getStatus()) || "MERGED".equalsIgnoreCase(duplicate.getStatus()))
            blockers.add("Un dossier est déjà fusionné");
        Map<String, Long> references = references(duplicateId);
        for (var e : references.entrySet())
            if (e.getValue() > 0 && !TRANSFERABLE.contains(e.getKey()))
                blockers.add("Relation non prise en charge : " + e.getKey() + " (" + e.getValue() + ")");
        Long collisions = jdbc.queryForObject("SELECT count(*) FROM student_guardian s JOIN student_guardian p ON s.student_id=p.student_id WHERE s.guardian_id=? AND p.guardian_id=?", Long.class, duplicateId, primaryId);
        if (collisions != null && collisions > 0) blockers.add("Enfants communs aux deux dossiers : fusion manuelle des rôles nécessaire");
        Long duplicateAccounts = jdbc.queryForObject("SELECT count(*) FROM user_account WHERE person_id=?", Long.class, duplicate.getPerson().getId());
        Long primaryAccounts = jdbc.queryForObject("SELECT count(*) FROM user_account WHERE person_id=?", Long.class, primary.getPerson().getId());
        if (duplicateAccounts != null && duplicateAccounts > 0 && (primaryAccounts == null || primaryAccounts == 0))
            blockers.add("Le compte du doublon serait désactivé sans compte principal : activer d'abord le compte principal");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primaryId", primaryId); result.put("duplicateId", duplicateId);
        result.put("primaryName", primary.getPerson().getFirstName() + " " + primary.getPerson().getLastName());
        result.put("duplicateName", duplicate.getPerson().getFirstName() + " " + duplicate.getPerson().getLastName());
        result.put("references", references); result.put("duplicateAccountsToDisable", duplicateAccounts);
        result.put("blockers", blockers); result.put("canMerge", blockers.isEmpty());
        return result;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, Object> merge(GuardianMergeController.MergeRequest request) {
        if (request == null || !request.confirmed())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation explicite requise");
        validate(request.primaryId(), request.duplicateId());
        // Verrouillage dans l'ordre des identifiants pour réduire les interblocages.
        jdbc.queryForList("SELECT id FROM guardian WHERE id IN (?, ?) ORDER BY id FOR UPDATE", request.primaryId(), request.duplicateId());
        Map<String, Object> inspection = inspect(request.primaryId(), request.duplicateId());
        @SuppressWarnings("unchecked") List<String> blockers = (List<String>) inspection.get("blockers");
        if (!blockers.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ; ", blockers));
        Long primaryId = request.primaryId(), duplicateId = request.duplicateId();
        // Les montants, numéros de reçus, imputations et identifiants restent inchangés.
        Map<String, Long> transferred = new LinkedHashMap<>();
        for (String relation : TRANSFERABLE) {
            String[] parts = relation.split("\\.");
            if (!tableColumnExists(parts[0], parts[1])) continue;
            int changed = jdbc.update("UPDATE \"" + parts[0] + "\" SET \"" + parts[1] + "\"=? WHERE \"" + parts[1] + "\"=?", primaryId, duplicateId);
            transferred.put(relation, (long) changed);
        }
        // Les identifiants d'authentification restent distincts, le doublon perd l'accès.
        Long duplicatePersonId = guardians.findById(duplicateId).orElseThrow().getPerson().getId();
        int accountsDisabled = jdbc.update("UPDATE user_account SET status='DISABLED', updated_at=now() WHERE person_id=? AND status <> 'DISABLED'", duplicatePersonId);
        jdbc.update("UPDATE guardian SET status='MERGED' WHERE id=?", duplicateId);
        jdbc.update("INSERT INTO guardian_merge_history(primary_guardian_id, duplicate_guardian_id, transferred_references, disabled_accounts) VALUES (?, ?, CAST(? AS jsonb), ?)",
            primaryId, duplicateId, toJson(transferred), accountsDisabled);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primaryId", primaryId); result.put("duplicateId", duplicateId);
        result.put("transferred", transferred); result.put("disabledAccounts", accountsDisabled);
        result.put("status", "MERGED");
        return result;
    }

    private String toJson(Map<String, Long> values) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        values.forEach((k, v) -> joiner.add("\"" + k + "\":" + v));
        return joiner.toString();
    }
    private void validate(Long primaryId, Long duplicateId) {
        if (primaryId == null || duplicateId == null || primaryId <= 0 || duplicateId <= 0 || primaryId.equals(duplicateId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deux identifiants distincts sont requis");
    }
    private boolean tableColumnExists(String table, String col) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name=? AND column_name=?", Integer.class, table, col);
        return n != null && n > 0;
    }
    private Map<String, Long> references(Long guardianId) {
        var cols = jdbc.queryForList("""
            SELECT table_name, column_name FROM information_schema.columns
            WHERE table_schema=current_schema()
              AND (column_name = 'guardian_id' OR column_name IN ('accepted_by_guardian_id','submitted_by_guardian_id'))
              AND table_name <> 'guardian' ORDER BY table_name, column_name
            """);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (var row : cols) {
            String table = (String) row.get("table_name"), col = (String) row.get("column_name");
            String sql = "SELECT count(*) FROM \"" + table.replace("\"", "\"\"") + "\" WHERE \"" + col.replace("\"", "\"\"") + "\"=?";
            counts.put(table + "." + col, jdbc.queryForObject(sql, Long.class, guardianId));
        }
        return counts;
    }
}
