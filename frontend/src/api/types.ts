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
