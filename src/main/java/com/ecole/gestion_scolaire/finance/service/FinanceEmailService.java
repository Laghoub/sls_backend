package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.receipt.PaymentReceiptResponse;
import com.ecole.gestion_scolaire.finance.entity.FinanceEmailOutbox;
import com.ecole.gestion_scolaire.finance.repository.FinanceEmailOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class FinanceEmailService {
    private static final String LOGO_CID = "slsLogo";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final FinanceEmailOutboxRepository outbox;
    private final PaymentReceiptService receipts;
    private final ApplicationContext context;

    @Value("${app.finance-email.enabled:true}")
    private boolean enabled;
    @Value("${app.finance-email.from:sls.school2008@gmail.com}")
    private String from;
    @Value("${app.finance-email.from-name:Groupe Scolaire Sciences et Lettres}")
    private String fromName;

    public FinanceEmailService(FinanceEmailOutboxRepository outbox,
                               PaymentReceiptService receipts,
                               ApplicationContext context) {
        this.outbox = outbox;
        this.receipts = receipts;
        this.context = context;
    }

    public void queuePaymentReceipt(Long paymentId) {
        var receipt = receipts.get(paymentId);
        if (!hasRecipient(receipt)) return;

        var email = new FinanceEmailOutbox();
        email.setRecipientEmail(receipt.guardianEmail().trim());
        email.setSubject("Reçu de paiement " + receipt.paymentNumber() + " - Groupe Scolaire Sciences et Lettres");
        email.setDocumentType("PAYMENT_RECEIPT");
        email.setDocumentId(paymentId);
        email.setBodyHtml(paymentHtml(receipt));
        email = outbox.save(email);
        scheduleDelivery(email.getId());
    }

    public void queueRefundReceipt(Long refundId, Long paymentId, BigDecimal refundedAmount,
                                   BigDecimal creditReversed, BigDecimal allocationReversed) {
        var receipt = receipts.get(paymentId);
        if (!hasRecipient(receipt)) return;

        var email = new FinanceEmailOutbox();
        email.setRecipientEmail(receipt.guardianEmail().trim());
        email.setSubject("Confirmation de remboursement - " + receipt.paymentNumber() + " - Groupe Scolaire Sciences et Lettres");
        email.setDocumentType("REFUND_RECEIPT");
        email.setDocumentId(refundId);
        email.setBodyHtml(refundHtml(receipt, refundedAmount, creditReversed, allocationReversed));
        email = outbox.save(email);
        scheduleDelivery(email.getId());
    }

    /**
     * Traite automatiquement la file d'attente des e-mails financiers.
     * Le premier passage a lieu 5 secondes après le démarrage, puis toutes les 30 secondes.
     * Ainsi, les messages PENDING existants sont repris sans refaire un paiement.
     */
    @Scheduled(initialDelayString = "${app.finance-email.dispatcher.initial-delay-ms:5000}",
               fixedDelayString = "${app.finance-email.dispatcher.fixed-delay-ms:30000}")
    public void dispatchPendingAutomatically() {
        if (!enabled) return;
        retryPending();
    }

    public int retryPending() {
        int sent = 0;
        for (var email : outbox.findTop50ByStatusOrderByCreatedAtAsc("PENDING")) {
            if (tryDeliver(email.getId())) sent++;
        }
        return sent;
    }

    private boolean hasRecipient(PaymentReceiptResponse receipt) {
        return receipt.guardianEmail() != null && !receipt.guardianEmail().isBlank();
    }

    private void scheduleDelivery(Long id) {
        if (!enabled) return;
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { tryDeliver(id); }
            });
        } else {
            tryDeliver(id);
        }
    }

    /**
     * L'envoi est volontairement découplé de la transaction financière : un problème Gmail
     * ne doit jamais annuler un paiement ou un remboursement déjà validé.
     *
     * La réflexion conserve le module Finance compilable même si le starter Mail n'est pas
     * présent dans un src isolé. Dans le projet Maven complet, spring-boot-starter-mail doit
     * être installé pour que l'envoi réel soit disponible.
     */
    public boolean tryDeliver(Long id) {
        if (!enabled) return false;
        var email = outbox.findById(id).orElse(null);
        if (email == null || "SENT".equals(email.getStatus())) return false;

        try {
            Class<?> senderClass = Class.forName("org.springframework.mail.javamail.JavaMailSender");
            Object sender = context.getBean(senderClass);
            Object mimeMessage = senderClass.getMethod("createMimeMessage").invoke(sender);
            Class<?> mimeMessageClass = Class.forName("jakarta.mail.internet.MimeMessage");
            Class<?> helperClass = Class.forName("org.springframework.mail.javamail.MimeMessageHelper");
            Object helper = helperClass.getConstructor(mimeMessageClass, boolean.class, String.class)
                    .newInstance(mimeMessage, true, "UTF-8");

            helperClass.getMethod("setTo", String.class).invoke(helper, email.getRecipientEmail());
            helperClass.getMethod("setSubject", String.class).invoke(helper, email.getSubject());
            helperClass.getMethod("setText", String.class, boolean.class).invoke(helper, email.getBodyHtml(), true);

            if (from != null && !from.isBlank()) {
                if (fromName != null && !fromName.isBlank()) {
                    helperClass.getMethod("setFrom", String.class, String.class)
                            .invoke(helper, from.trim(), fromName.trim());
                } else {
                    helperClass.getMethod("setFrom", String.class).invoke(helper, from.trim());
                }
            }

            // Logo officiel intégré dans l'e-mail via CID (pas de lien externe nécessaire).
            Resource logo = new ClassPathResource("sls-logo.png");
            if (logo.exists()) {
                helperClass.getMethod("addInline", String.class, Resource.class)
                        .invoke(helper, LOGO_CID, logo);
            }

            senderClass.getMethod("send", mimeMessageClass).invoke(sender, mimeMessage);
            email.setStatus("SENT");
            email.setSentAt(OffsetDateTime.now());
            email.setLastError(null);
            email.setAttemptCount(email.getAttemptCount() + 1);
            outbox.save(email);
            return true;
        } catch (Exception ex) {
            email.setStatus("PENDING");
            email.setAttemptCount(email.getAttemptCount() + 1);
            String msg = ex.getMessage();
            if (msg == null && ex.getCause() != null) msg = ex.getCause().getMessage();
            email.setLastError(msg == null ? ex.getClass().getSimpleName() : msg.substring(0, Math.min(1800, msg.length())));
            outbox.save(email);
            return false;
        }
    }

    private String paymentHtml(PaymentReceiptResponse r) {
        StringBuilder rows = new StringBuilder();
        for (var line : r.allocations()) {
            rows.append("<tr><td>")
                    .append(esc(studentName(line.studentFirstName(), line.studentLastName())))
                    .append("</td><td>").append(esc(line.chargeLabel()))
                    .append("</td><td class='amount'>").append(money(line.amount()))
                    .append("</td></tr>");
        }

        String reference = blankToDash(r.externalReference());
        String method = blankToDash(r.paymentMethodName());
        String date = r.paymentDate() == null ? "—" : r.paymentDate().format(DATE_TIME);

        return shell("Reçu de paiement", "Paiement enregistré avec succès", """
                <p class='hello'>Bonjour <strong>%s %s</strong>,</p>
                <p>Nous confirmons la réception de votre règlement auprès du <strong>Groupe Scolaire Sciences et Lettres</strong>.</p>

                <div class='info-grid'>
                  <div><span>N° de reçu</span><strong>%s</strong></div>
                  <div><span>Date</span><strong>%s</strong></div>
                  <div><span>Mode de paiement</span><strong>%s</strong></div>
                  <div><span>Référence externe</span><strong>%s</strong></div>
                </div>

                <h3>Détail des frais réglés</h3>
                <table><thead><tr><th>Élève</th><th>Frais / créance</th><th class='amount'>Montant</th></tr></thead>
                <tbody>%s</tbody></table>

                <div class='totals'>
                  <div><span>Total des frais réglés</span><strong>%s</strong></div>
                  <div><span>Montant reçu</span><strong>%s</strong></div>
                  <div><span>Montant placé dans l'avoir familial</span><strong>%s</strong></div>
                  <div class='grand'><span>Net encaissé à ce jour</span><strong>%s</strong></div>
                </div>

                %s
                <p class='note'>Ce message constitue un justificatif électronique de l'opération enregistrée dans le système de gestion scolaire. Conservez-le pour vos archives.</p>
                """.formatted(
                esc(r.guardianFirstName()), esc(r.guardianLastName()), esc(r.paymentNumber()), esc(date),
                esc(method), esc(reference), rows,
                money(r.allocatedAmount()), money(r.totalAmount()), money(r.creditCreatedAmount()), money(r.netReceivedAmount()),
                creditNote(r.creditCreatedAmount(), r.creditRemainingAmount())
        ));
    }

    private String refundHtml(PaymentReceiptResponse r, BigDecimal amount,
                              BigDecimal creditReversed, BigDecimal allocationReversed) {
        StringBuilder reopenedRows = new StringBuilder();
        for (var line : r.allocations()) {
            if (line.refundedAmount() == null || line.refundedAmount().signum() <= 0) continue;
            reopenedRows.append("<tr><td>")
                    .append(esc(studentName(line.studentFirstName(), line.studentLastName())))
                    .append("</td><td>").append(esc(line.chargeLabel()))
                    .append("</td><td class='amount'>").append(money(line.refundedAmount()))
                    .append("</td><td class='amount'>").append(money(line.netAmount()))
                    .append("</td></tr>");
        }

        String reopenedSection = reopenedRows.isEmpty() ? "" : """
                <h3>Créances concernées par le remboursement</h3>
                <table><thead><tr><th>Élève</th><th>Frais / créance</th><th class='amount'>Réouvert</th><th class='amount'>Payé net restant</th></tr></thead>
                <tbody>%s</tbody></table>
                """.formatted(reopenedRows);

        return shell("Confirmation de remboursement", "Remboursement enregistré", """
                <p class='hello'>Bonjour <strong>%s %s</strong>,</p>
                <p>Nous confirmons qu'un remboursement lié au paiement <strong>%s</strong> a été enregistré.</p>

                <div class='totals refund'>
                  <div class='grand'><span>Montant de ce remboursement</span><strong>%s</strong></div>
                  <div><span>Prélevé sur l'avoir familial</span><strong>%s</strong></div>
                  <div><span>Montant ayant réouvert des créances</span><strong>%s</strong></div>
                  <div><span>Total remboursé sur le paiement</span><strong>%s</strong></div>
                  <div><span>Net encaissé restant</span><strong>%s</strong></div>
                  <div><span>Avoir familial restant issu du paiement</span><strong>%s</strong></div>
                </div>

                %s
                <p class='note'>Lorsqu'une somme déjà affectée à une créance est remboursée, cette créance est réouverte à hauteur du montant contre-passé. La partie provenant d'un avoir familial diminue directement cet avoir.</p>
                """.formatted(
                esc(r.guardianFirstName()), esc(r.guardianLastName()), esc(r.paymentNumber()),
                money(amount), money(creditReversed), money(allocationReversed), money(r.refundedAmount()),
                money(r.netReceivedAmount()), money(r.creditRemainingAmount()), reopenedSection
        ));
    }

    private String creditNote(BigDecimal created, BigDecimal remaining) {
        if (created == null || created.signum() <= 0) return "";
        return """
                <div class='credit-box'><strong>Information avoir familial</strong><br>
                Le règlement dépassait le total des frais affectés. <strong>%s</strong> ont donc été placés dans l'avoir familial.
                Solde actuel lié à ce paiement : <strong>%s</strong>.</div>
                """.formatted(money(created), money(remaining));
    }

    private String shell(String title, String subtitle, String body) {
        return """
            <!doctype html>
            <html lang='fr'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
            <style>
              body{margin:0;background:#f3f6fb;font-family:Arial,Helvetica,sans-serif;color:#172033}
              .wrap{max-width:760px;margin:0 auto;padding:24px 12px}.card{background:#fff;border-radius:18px;overflow:hidden;box-shadow:0 8px 28px rgba(15,39,66,.08)}
              .header{background:#0f2742;padding:24px 28px;color:#fff;display:flex;align-items:center;gap:18px}.header img{width:92px;height:68px;object-fit:contain;background:#fff;border-radius:12px;padding:5px}
              .school{font-size:19px;font-weight:700}.subtitle{font-size:13px;opacity:.82;margin-top:5px}.content{padding:28px}.hello{font-size:16px}
              h3{color:#0f2742;margin:26px 0 10px}table{width:100%%;border-collapse:collapse;margin:8px 0 22px}th{background:#f5f7fb;color:#475467;font-size:12px;text-transform:uppercase;letter-spacing:.03em}
              th,td{padding:11px 10px;border-bottom:1px solid #e8edf4;text-align:left}.amount{text-align:right;white-space:nowrap}
              .info-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:22px 0}.info-grid div,.totals div{background:#f7f9fc;border:1px solid #e8edf4;border-radius:10px;padding:12px 14px}
              .info-grid span,.totals span{display:block;font-size:12px;color:#667085;margin-bottom:5px}.totals{display:grid;grid-template-columns:1fr 1fr;gap:10px}.totals .grand{background:#edf5ff;border-color:#cfe2ff}.totals .grand strong{font-size:18px;color:#0f5fc2}
              .refund .grand{background:#fff4ed;border-color:#ffd7bd}.refund .grand strong{color:#b54708}.credit-box{margin-top:18px;padding:14px 16px;border-left:4px solid #d4a017;background:#fffaf0;border-radius:8px}
              .note{margin-top:24px;font-size:12px;line-height:1.55;color:#667085}.footer{text-align:center;color:#667085;font-size:12px;padding:18px}.footer strong{color:#0f2742}
              @media(max-width:600px){.info-grid,.totals{grid-template-columns:1fr}.content{padding:20px}.header{padding:20px}.header img{width:72px;height:54px}th,td{font-size:12px;padding:9px 6px}}
            </style><title>%s</title></head>
            <body><div class='wrap'><div class='card'>
              <div class='header'><img src='cid:%s' alt='Logo SLS'><div><div class='school'>Groupe Scolaire Sciences et Lettres</div><div class='subtitle'>%s</div></div></div>
              <div class='content'>%s</div>
              <div class='footer'><strong>Groupe Scolaire Sciences et Lettres</strong><br>sls.school2008@gmail.com</div>
            </div></div></body></html>
            """.formatted(esc(title), LOGO_CID, esc(subtitle), body);
    }

    private String studentName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName + " ") + (lastName == null ? "" : lastName)).trim();
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String money(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        NumberFormat f = NumberFormat.getNumberInstance(Locale.FRANCE);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(value) + " DA";
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
