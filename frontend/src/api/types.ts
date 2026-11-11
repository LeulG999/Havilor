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
