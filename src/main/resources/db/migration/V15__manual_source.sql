-- Flow 19: alerts an analyst enters by hand (source MANUAL), after Havilor extracted a draft from an email,
-- headers, a screenshot or a PDF.
ALTER TABLE alert DROP CONSTRAINT ck_alert_source;
ALTER TABLE alert ADD CONSTRAINT ck_alert_source
    CHECK (source IN ('SERVICENOW', 'PROOFPOINT_TAP', 'TEST_GENERATOR', 'MANUAL'));
