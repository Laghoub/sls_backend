CREATE TABLE attendance_email_outbox (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(300) NOT NULL,
    body_html TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    attendance_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ
);
CREATE INDEX idx_attendance_email_outbox_pending ON attendance_email_outbox(status, created_at);
CREATE INDEX idx_attendance_email_outbox_attendance ON attendance_email_outbox(notification_type, attendance_id);
