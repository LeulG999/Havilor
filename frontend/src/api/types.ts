// The shapes of the JSON the backend sends (its response DTOs), so TypeScript can check how we use them.
// Only the fields the pages show are listed.

export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

export type User = {
  id: number;
  name: string;
  email: string;
  role: string;
};

// The backend's page shape (PageResponse) for every list endpoint.
export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

// One row of GET /api/v1/alerts (AlertSummaryResponse).
export type AlertSummary = {
  id: number;
  ticketNumber: string;
  priority: string;
  status: string;
  subject: string;
  receivedAt: string; // an ISO date-time, e.g. "2026-10-28T14:00:00Z"
};

// GET /api/v1/alerts/{id} (AlertDetailResponse) and its parts.
export type AlertDetail = {
  id: number;
  ticketNumber: string;
  source: string;
  caseType: string;
  priority: string;
  status: string;
  verdict: string;
  assigneeId: number | null;
  suggestedVerdict: string | null;
  suggestionReason: string | null;
  receivedAt: string;
  closedAt: string | null;
  email: EmailDetails;
  recipients: Recipient[];
  urls: AlertUrl[];
  attachments: Attachment[];
  clicks: Click[];
};

export type EmailDetails = {
  subject: string;
  fromAddress: string;
  envelopeSender: string | null;
  replyTo: string | null;
  spfResult: string | null;
  dkimResult: string | null;
  dmarcResult: string | null;
  phishScore: number | null;
};

export type Recipient = { id: number; address: string; delivery: string };
export type AlertUrl = { id: number; address: string; domain: string | null; onBadList: boolean };
export type Attachment = { id: number; fileName: string; fileType: string | null; sha256: string | null; verdict: string | null };
export type Click = { id: number; recipientAddress: string; urlAddress: string; clickedAt: string; permitted: boolean };

// GET /api/v1/alerts/{id}/findings
export type Finding = {
  id: number;
  type: string;
  severity: string;
  title: string;
  teachingText: string | null;
  source: string;
};

// GET /api/v1/alerts/{id}/steps
export type AlertStep = {
  stepId: number;
  orderNo: number;
  title: string;
  whyItMatters: string;
  question: string;
  answerOptions: string[]; // empty when the answer is free text
  status: string;
  answer: string | null;
};

// GET /api/v1/alerts/{id}/actions
export type Action = {
  id: number;
  type: string;
  result: string;
  note: string | null;
  performedBy: number;
  performedAt: string;
};

// POST /api/v1/alerts/{id}/ai/assessment
export type AiAssessment = {
  summary: string;
  suggestedVerdict: string;
  suggestionReason: string;
  // The knowledge Claude was given: a past closed alert (sourceId = alert id) or a playbook step (step id).
  references: { sourceType: 'CLOSED_ALERT' | 'PLAYBOOK_STEP'; sourceId: number }[];
};

// POST /api/v1/alerts/{id}/ai/draft-email
export type EmailDraft = {
  type: string;
  draft: string;
};

// GET /api/v1/alerts/dashboard
export type Dashboard = {
  openAlerts: number;
  assignedToMe: number;
  criticalOpen: number;
  closedLast7Days: number;
  alertsPerDay: { date: string; count: number }[]; // 14 UTC days, oldest first; date like "2026-11-01"
  recent: AlertSummary[];
};

// A draft alert: what POST /api/v1/alerts/manual/extract returns, and what POST /api/v1/alerts accepts
// (the backend's CreateAlertRequest). null = unknown / not filled in.
export type AlertDraft = {
  ticketNumber: string | null; // empty for a manual alert: the backend generates MAN-<date>-<n>
  source: string;
  caseType: string;
  priority: string;
  receivedAt: string | null;
  email: {
    subject: string;
    fromAddress: string;
    envelopeSender: string | null;
    senderHeader: string | null;
    replyTo: string | null;
    xMailer: string | null;
    spfResult: string | null;
    dkimResult: string | null;
    dmarcResult: string | null;
    phishScore: number | null;
    spamScore: number | null;
    filterAction: string | null;
    quarantineFolder: string | null;
    rawHeaders: string | null;
  };
  recipients: { address: string; delivery: string }[];
  urls: { address: string; domain: string | null; onBadList: boolean | null }[];
  attachments: { fileName: string; fileType: string | null; sha256: string | null; verdict: string | null }[];
  clicks: unknown[];
};

// GET /api/v1/alerts/{id}/escalations (EscalationResponse): the alert was handed to another team.
export type Escalation = {
  id: number;
  alertId: number;
  team: string;
  reason: string;
  externalStatus: string; // OPEN, IN_PROGRESS, RESOLVED: how far the other team is
  outcome: string | null; // CONFIRMED_MALICIOUS, NOT_MALICIOUS, INCONCLUSIVE: their answer (null = none yet)
};
