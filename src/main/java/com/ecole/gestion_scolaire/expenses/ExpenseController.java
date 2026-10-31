package com.ecole.gestion_scolaire.expenses;

import com.ecole.gestion_scolaire.finance.service.CurrentFinanceAccountService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/finance/expenses")
public class ExpenseController {
    private final JdbcTemplate db;
    private final CurrentFinanceAccountService current;
    public ExpenseController(JdbcTemplate db, CurrentFinanceAccountService current) { this.db=db; this.current=current; }
    public record ExpenseInput(LocalDate expenseDate, Long categoryId, Long supplierId, String description, BigDecimal amount) {}
    public record PaymentInput(BigDecimal amount, String paymentMethod, Long cashSessionId) {}
    public record SupplierInput(String name,String phone,String email,String address,String taxIdentifier,Boolean active) {}
    public record CategoryInput(String name,Long parentId,Boolean active) {}
    public record BudgetInput(Long categoryId,Integer year,Integer month,BigDecimal amount) {}
    public record AttachmentInput(String filename,String storageKey,String mimeType) {}
    private void require(boolean ok, String message) { if (!ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private Map<String,Object> row(String sql,Object... args) { try { return db.queryForMap(sql,args); } catch(org.springframework.dao.EmptyResultDataAccessException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Élément introuvable"); } }
    private Long id(String sql,Object... args) { return db.queryForObject(sql,Long.class,args); }
    @GetMapping @PreAuthorize("hasAuthority('DEPENSES_CONSULTER')")
    public List<Map<String,Object>> list(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) String status) {
        return db.queryForList("SELECT e.*,c.name category_name,s.name supplier_name FROM expense e JOIN expense_category c ON c.id=e.expense_category_id LEFT JOIN provider s ON s.id=e.provider_id WHERE (?::date IS NULL OR e.expense_date>=?::date) AND (?::date IS NULL OR e.expense_date<=?::date) AND (?::bigint IS NULL OR e.expense_category_id=?::bigint) AND (?::text IS NULL OR e.status=?::text) ORDER BY e.expense_date DESC,e.id DESC LIMIT 1000",from,from,to,to,categoryId,categoryId,status,status);
    }
    @GetMapping("/{expenseId}") @PreAuthorize("hasAuthority('DEPENSES_CONSULTER')")
    public Map<String,Object> one(@PathVariable Long expenseId) { return row("SELECT * FROM expense WHERE id=?",expenseId); }
    @PostMapping @Transactional @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> create(@RequestBody ExpenseInput r) {
        require(r.expenseDate()!=null && r.categoryId()!=null && r.description()!=null && !r.description().isBlank() && r.amount()!=null && r.amount().signum()>0,"Dépense incomplète");
        Long key=id("INSERT INTO expense(expense_date,expense_category_id,provider_id,description,amount,requested_by,status,expense_number,created_at,updated_at) VALUES (?,?,?,?,?,? ,'DRAFT','TMP-'||nextval('expense_reference_seq'),now(),now()) RETURNING id",r.expenseDate(),r.categoryId(),r.supplierId(),r.description(),r.amount(),current.id());
        db.update("UPDATE expense SET expense_number='DEP-'||EXTRACT(YEAR FROM expense_date)::int||'-'||lpad(id::text,8,'0') WHERE id=?",key);
        return one(key);
    }
    @PutMapping("/{expenseId}") @Transactional @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> update(@PathVariable Long expenseId,@RequestBody ExpenseInput r) {
        require("DRAFT".equals(row("SELECT status FROM expense WHERE id=? FOR UPDATE",expenseId).get("status")),"Seul un brouillon peut être modifié");
        require(r.expenseDate()!=null && r.categoryId()!=null && r.description()!=null && !r.description().isBlank() && r.amount()!=null && r.amount().signum()>0,"Dépense incomplète");
        db.update("UPDATE expense SET expense_date=?,expense_category_id=?,provider_id=?,description=?,amount=?,updated_at=now() WHERE id=?",r.expenseDate(),r.categoryId(),r.supplierId(),r.description(),r.amount(),expenseId);
        return one(expenseId);
    }
    @PostMapping("/{expenseId}/submit") @Transactional @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> submit(@PathVariable Long expenseId) { require(db.update("UPDATE expense SET status='PENDING',updated_at=now() WHERE id=? AND status='DRAFT'",expenseId)==1,"Brouillon requis"); return one(expenseId); }
    @PostMapping("/{expenseId}/approve") @Transactional @PreAuthorize("hasAuthority('DEPENSES_VALIDER')")
    public Map<String,Object> approve(@PathVariable Long expenseId) { require(db.update("UPDATE expense SET status='APPROVED',validated_by=?,validated_at=now(),updated_at=now() WHERE id=? AND status='PENDING'",current.id(),expenseId)==1,"Dépense en attente requise"); return one(expenseId); }
    @PostMapping("/{expenseId}/reject") @Transactional @PreAuthorize("hasAuthority('DEPENSES_VALIDER')")
    public Map<String,Object> reject(@PathVariable Long expenseId,@RequestBody Map<String,String> body) { require(db.update("UPDATE expense SET status='REJECTED',rejected_reason=?,updated_at=now() WHERE id=? AND status='PENDING'",body.getOrDefault("reason",""),expenseId)==1,"Dépense en attente requise"); return one(expenseId); }
    @PostMapping("/{expenseId}/payments") @Transactional @PreAuthorize("hasAuthority('DEPENSES_PAYER')")
    public Map<String,Object> pay(@PathVariable Long expenseId,@RequestBody PaymentInput r) {
        Map<String,Object> expense=row("SELECT * FROM expense WHERE id=? FOR UPDATE",expenseId);
        require(Set.of("APPROVED","PARTIALLY_PAID").contains(expense.get("status")),"Validation requise avant paiement");
        require(r.amount()!=null && r.amount().signum()>0 && r.amount().compareTo(((BigDecimal)expense.get("amount")).subtract((BigDecimal)expense.get("paid_amount")))<=0,"Montant invalide");
        require(r.paymentMethod()!=null && !r.paymentMethod().isBlank(),"Mode de paiement requis");
        boolean cash="CASH".equalsIgnoreCase(r.paymentMethod()) || "ESPECES".equalsIgnoreCase(r.paymentMethod());
        if(cash) { require(r.cashSessionId()!=null,"Session de caisse requise"); Map<String,Object> session=row("SELECT status,cashier_id FROM cash_register_session WHERE id=? FOR UPDATE",r.cashSessionId()); require("OPEN".equals(session.get("status")) && current.id().equals(((Number)session.get("cashier_id")).longValue()),"Session de caisse non autorisée ou fermée"); }
        Long paymentId=id("INSERT INTO expense_payment(expense_id,amount,payment_method,cash_register_session_id,paid_by) VALUES (?,?,?,?,?) RETURNING id",expenseId,r.amount(),r.paymentMethod(),cash?r.cashSessionId():null,current.id());
        String reference="DEP-PAY-"+String.format("%08d",paymentId);
        db.update("UPDATE expense_payment SET reference=? WHERE id=?",reference,paymentId);
        if(cash) db.update("INSERT INTO cash_movement(cash_register_session_id,movement_type,direction,amount,expense_payment_id,reference,description,created_by,created_at) VALUES (?,'EXPENSE','OUT',?,?,?,?,?,now())",r.cashSessionId(),r.amount(),paymentId,reference,expense.get("description"),current.id());
        db.update("UPDATE expense SET paid_amount=paid_amount+?,status=CASE WHEN paid_amount+? >= amount THEN 'PAID' ELSE 'PARTIALLY_PAID' END,updated_at=now() WHERE id=?",r.amount(),r.amount(),expenseId);
        return row("SELECT * FROM expense_payment WHERE id=?",paymentId);
    }
    @GetMapping("/{expenseId}/payments") @PreAuthorize("hasAuthority('DEPENSES_CONSULTER')")
    public List<Map<String,Object>> payments(@PathVariable Long expenseId) { return db.queryForList("SELECT * FROM expense_payment WHERE expense_id=? ORDER BY paid_at",expenseId); }
    @GetMapping("/categories") @PreAuthorize("hasAuthority('DEPENSES_CONSULTER')")
    public List<Map<String,Object>> categories() { return db.queryForList("SELECT * FROM expense_category ORDER BY name"); }
    @PostMapping("/categories") @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> category(@RequestBody CategoryInput r) { require(r.name()!=null&&!r.name().isBlank(),"Nom requis"); Long key=id("INSERT INTO expense_category(name,parent_category_id,active,code) VALUES (?,?,?,'CUSTOM-'||nextval('expense_category_code_seq')) RETURNING id",r.name(),r.parentId(),r.active()==null||r.active()); return row("SELECT * FROM expense_category WHERE id=?",key); }
    @GetMapping("/suppliers") @PreAuthorize("hasAuthority('DEPENSES_CONSULTER')")
    public List<Map<String,Object>> suppliers() { return db.queryForList("SELECT * FROM provider ORDER BY name"); }
    @PostMapping("/suppliers") @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> supplier(@RequestBody SupplierInput r) { require(r.name()!=null&&!r.name().isBlank(),"Nom requis"); Long key=id("INSERT INTO provider(name,phone,email,address,tax_identifier,status,code,created_at,updated_at) VALUES (?,?,?,?,?,CASE WHEN ? THEN 'ACTIVE' ELSE 'INACTIVE' END,'SUP-'||nextval('expense_provider_code_seq'),now(),now()) RETURNING id",r.name(),r.phone(),r.email(),r.address(),r.taxIdentifier(),r.active()==null||r.active()); return row("SELECT * FROM provider WHERE id=?",key); }
    @PutMapping("/suppliers/{supplierId}") @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> supplierUpdate(@PathVariable Long supplierId,@RequestBody SupplierInput r) { require(r.name()!=null&&!r.name().isBlank(),"Nom requis"); require(db.update("UPDATE provider SET name=?,phone=?,email=?,address=?,tax_identifier=?,status=CASE WHEN ? THEN 'ACTIVE' ELSE 'INACTIVE' END,updated_at=now() WHERE id=?",r.name(),r.phone(),r.email(),r.address(),r.taxIdentifier(),r.active()==null||r.active(),supplierId)==1,"Fournisseur introuvable"); return row("SELECT * FROM provider WHERE id=?",supplierId); }
    @PostMapping("/budgets") @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> budget(@RequestBody BudgetInput r) { require(r.categoryId()!=null&&r.year()!=null&&r.month()!=null&&r.month()>=1&&r.month()<=12&&r.amount()!=null&&r.amount().signum()>=0,"Budget invalide"); db.update("INSERT INTO expense_budget(category_id,year,month,amount) VALUES (?,?,?,?) ON CONFLICT(category_id,year,month) DO UPDATE SET amount=EXCLUDED.amount",r.categoryId(),r.year(),r.month(),r.amount()); return row("SELECT * FROM expense_budget WHERE category_id=? AND year=? AND month=?",r.categoryId(),r.year(),r.month()); }
    @GetMapping("/reports/budget") @PreAuthorize("hasAuthority('DEPENSES_RAPPORTS')")
    public List<Map<String,Object>> budgetReport(@RequestParam int year,@RequestParam int month) { return db.queryForList("SELECT c.id category_id,c.name,coalesce(b.amount,0) budget,coalesce((SELECT sum(e.amount) FROM expense e WHERE e.expense_category_id=c.id AND e.status NOT IN ('DRAFT','PENDING','REJECTED') AND extract(year from e.expense_date)=? AND extract(month from e.expense_date)=?),0) committed FROM expense_category c LEFT JOIN expense_budget b ON b.category_id=c.id AND b.year=? AND b.month=? ORDER BY c.name",year,month,year,month); }
    @GetMapping("/reports/cashflow") @PreAuthorize("hasAuthority('DEPENSES_RAPPORTS')")
    public List<Map<String,Object>> cashflow(@RequestParam LocalDate from,@RequestParam LocalDate to) { return db.queryForList("SELECT movement_type,direction,count(*) transactions,sum(amount) amount FROM cash_movement WHERE created_at>=?::date AND created_at < (?::date + interval '1 day') GROUP BY movement_type,direction ORDER BY movement_type,direction",from,to); }
    @GetMapping("/reports/categories") @PreAuthorize("hasAuthority('DEPENSES_RAPPORTS')")
    public List<Map<String,Object>> categoryReport(@RequestParam LocalDate from,@RequestParam LocalDate to) { return db.queryForList("SELECT c.name,count(e.id) count,coalesce(sum(e.amount),0) committed,coalesce(sum(e.paid_amount),0) paid FROM expense_category c LEFT JOIN expense e ON e.expense_category_id=c.id AND e.expense_date BETWEEN ? AND ? AND e.status NOT IN ('DRAFT','PENDING','REJECTED') GROUP BY c.id,c.name ORDER BY committed DESC",from,to); }
    @GetMapping("/reports/salary-cashflow") @PreAuthorize("hasAuthority('DEPENSES_RAPPORTS')")
    public List<Map<String,Object>> salaryCashflow(@RequestParam LocalDate from,@RequestParam LocalDate to) { return db.queryForList("SELECT CASE WHEN staff_pay_payment_id IS NOT NULL THEN 'STAFF' WHEN salary_payment_id IS NOT NULL THEN 'TEACHER' ELSE 'OTHER' END salary_type,count(*) transactions,sum(amount) paid FROM cash_movement WHERE direction='OUT' AND (staff_pay_payment_id IS NOT NULL OR salary_payment_id IS NOT NULL) AND created_at>=?::date AND created_at < (?::date + interval '1 day') GROUP BY 1",from,to); }
    @PostMapping("/{expenseId}/attachments") @PreAuthorize("hasAuthority('DEPENSES_GERER')")
    public Map<String,Object> attachment(@PathVariable Long expenseId,@RequestBody AttachmentInput r) { require(r.filename()!=null&&!r.filename().isBlank()&&r.storageKey()!=null&&!r.storageKey().isBlank(),"Fichier et clé de stockage requis"); row("SELECT id FROM expense WHERE id=?",expenseId); Long key=id("INSERT INTO expense_attachment(expense_id,filename,storage_key,mime_type,uploaded_by) VALUES (?,?,?,?,?) RETURNING id",expenseId,r.filename(),r.storageKey(),r.mimeType(),current.id()); return row("SELECT * FROM expense_attachment WHERE id=?",key); }
    @GetMapping("/{expenseId}/attachments") @PreAuthorize("hasAuthority('DEPENSES_CONSULTER')")
    public List<Map<String,Object>> attachments(@PathVariable Long expenseId) { return db.queryForList("SELECT id,expense_id,filename,mime_type,created_at FROM expense_attachment WHERE expense_id=? ORDER BY id",expenseId); }
}
