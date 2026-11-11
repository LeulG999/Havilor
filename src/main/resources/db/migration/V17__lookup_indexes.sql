-- Flow 22: indexes for the lookups the app runs on every alert view, found missing in the performance review.
-- Without them PostgreSQL scans the whole table (it grows with every alert); measured on 300,000 synthetic rows:
-- a full scan of 2,703 pages (~10 ms) became an index scan of 12 pages (~0.07 ms). See docs/progress/flow-22.md.

-- GET /alerts/{id}/findings, the AI assessment, and closing (findings for the knowledge text)
CREATE INDEX idx_finding_alert_id ON finding (alert_id);

-- GET /alerts/{id}/actions
CREATE INDEX idx_action_alert_id ON action (alert_id);

-- GET /alerts/{id}/escalations
CREATE INDEX idx_escalation_alert_id ON escalation (alert_id);

-- GET /alerts/{id}/audit (WHERE entity_type = 'ALERT' AND entity_id = ?); the audit log grows fastest of all
CREATE INDEX idx_audit_entry_entity ON audit_entry (entity_type, entity_id);
