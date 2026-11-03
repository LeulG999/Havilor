-- Flow 4: the alert gets every column from the ERD except playbook_id (added in Flow 8 with the playbook link).
-- Columns that are NOT NULL get a DEFAULT so rows that already exist can be filled in. The defaults are then
-- dropped again: from now on the application must set these values itself.

ALTER TABLE alert
    ADD COLUMN source               VARCHAR(30)  NOT NULL DEFAULT 'TEST_GENERATOR',
    ADD COLUMN case_type            VARCHAR(30)  NOT NULL DEFAULT 'DETECTED_PHISHING',
    ADD COLUMN priority             VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN verdict              VARCHAR(30)  NOT NULL DEFAULT 'UNDECIDED',
    ADD COLUMN suggested_verdict    VARCHAR(30),
    ADD COLUMN suggestion_reason    TEXT,
    ADD COLUMN assignee_id          BIGINT,           -- FK to app_user comes with the user module (Slice 4)
    ADD COLUMN waiting_on           VARCHAR(200),
    ADD COLUMN external_case_number VARCHAR(50),
    ADD COLUMN follow_up_on         DATE,
    ADD COLUMN closed_at            TIMESTAMPTZ,
    ADD COLUMN summary              TEXT,
    -- EmailDetails (embedded): these live in the alert table, there is no email table.
    -- subject already exists from V1.
    ADD COLUMN from_address         VARCHAR(320),
    ADD COLUMN envelope_sender      VARCHAR(320),
    ADD COLUMN sender_header        VARCHAR(500),
    ADD COLUMN reply_to             VARCHAR(320),
    ADD COLUMN x_mailer             VARCHAR(200),
    ADD COLUMN spf_result           VARCHAR(10),
    ADD COLUMN dkim_result          VARCHAR(10),
    ADD COLUMN dmarc_result         VARCHAR(10),
    ADD COLUMN phish_score          INTEGER,
    ADD COLUMN spam_score           INTEGER,
    ADD COLUMN filter_action        VARCHAR(50),
    ADD COLUMN quarantine_folder    VARCHAR(100),
    ADD COLUMN raw_headers          TEXT,
    ADD COLUMN created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ADD COLUMN updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now();

ALTER TABLE alert
    ALTER COLUMN source     DROP DEFAULT,
    ALTER COLUMN case_type  DROP DEFAULT,
    ALTER COLUMN priority   DROP DEFAULT,
    ALTER COLUMN verdict    DROP DEFAULT,
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN updated_at DROP DEFAULT;

-- Enums are stored as their name (@Enumerated(STRING)). The CHECKs stop a typo or a bad manual UPDATE
-- from storing a value Java can't read back. A new enum value needs a migration that widens its CHECK.
ALTER TABLE alert
    ADD CONSTRAINT ck_alert_status CHECK (status IN ('NEW', 'INVESTIGATING', 'ESCALATED', 'WAITING_ON_EXTERNAL', 'CLOSED')),
    ADD CONSTRAINT ck_alert_source CHECK (source IN ('SERVICENOW', 'PROOFPOINT_TAP', 'TEST_GENERATOR')),
    ADD CONSTRAINT ck_alert_case_type CHECK (case_type IN ('DETECTED_PHISHING', 'USER_REPORTED_EMAIL', 'RELEASE_REQUEST')),
    ADD CONSTRAINT ck_alert_priority CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    ADD CONSTRAINT ck_alert_verdict CHECK (verdict IN ('UNDECIDED', 'MALICIOUS', 'BENIGN', 'FALSE_POSITIVE')),
    ADD CONSTRAINT ck_alert_suggested_verdict CHECK (suggested_verdict IN ('UNDECIDED', 'MALICIOUS', 'BENIGN', 'FALSE_POSITIVE')),
    ADD CONSTRAINT ck_alert_spf CHECK (spf_result IN ('PASS', 'FAIL', 'NONE')),
    ADD CONSTRAINT ck_alert_dkim CHECK (dkim_result IN ('PASS', 'FAIL', 'NONE')),
    ADD CONSTRAINT ck_alert_dmarc CHECK (dmarc_result IN ('PASS', 'FAIL', 'NONE')),
    ADD CONSTRAINT ck_alert_phish_score CHECK (phish_score BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_alert_spam_score CHECK (spam_score BETWEEN 0 AND 100);
