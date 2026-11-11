// The only place that talks to the backend. Every page calls api(...) instead of fetch(...) directly,
// so the token, the JSON handling and the error handling are written once.

const TOKEN_KEY = 'havilor.token';

// sessionStorage: the token is gone when the browser tab closes, and it is not sent automatically like a cookie.
export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function saveToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY);
}

// The backend answers every error with the same JSON shape (ApiError in GlobalExceptionHandler).
// We keep its message, so the user sees exactly what the backend said.
export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// Calls /api/v1 + path. Sends the token (if we have one) and a JSON body (if given); returns the JSON answer.
export async function api<T>(path: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`/api/v1${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  // 401 with a token = the token expired or is no longer valid: forget it and go to the login page.
  // (A 401 without a token is a failed login; the login page shows that message itself.)
  if (response.status === 401 && token) {
    clearToken();
    window.location.assign('/login');
  }

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(response.status, error?.message ?? `Request failed (${response.status})`);
  }
  return response.json() as Promise<T>;
}
