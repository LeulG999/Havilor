import { useState, type FormEvent } from 'react';
import { api, saveToken } from '../api/api';
import type { LoginResponse } from '../api/types';
import ErrorMessage from './ErrorMessage';

const MIN_LENGTH = 12;

// "Set / change your password": POST /api/v1/users/me/password {currentPassword, newPassword}. Used on the
// first-login screen (current = the temporary password) and from the avatar menu. The backend answers with a NEW
// token (without "must change password"), which we keep, so the person can carry on without logging in again.
export default function ChangePasswordForm({ currentLabel, onDone }: { currentLabel: string; onDone: () => void }) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [repeat, setRepeat] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  // Checked here first so she sees it right away; the backend checks the length again (and the current password).
  const mismatch = repeat !== '' && repeat !== next;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (mismatch) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const answer = await api<LoginResponse>('/users/me/password', {
        method: 'POST',
        body: { currentPassword: current, newPassword: next },
      });
      saveToken(answer.accessToken);
      onDone();
    } catch (e) {
      setError(e); // e.g. 400 "Validation failed: currentPassword is not correct"
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="password-form">
      <label>
        {currentLabel}
        <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} required
               autoComplete="current-password" />
      </label>
      <label>
        New password (at least {MIN_LENGTH} characters)
        <input type="password" value={next} onChange={(e) => setNext(e.target.value)} required minLength={MIN_LENGTH}
               autoComplete="new-password" />
      </label>
      <label>
        New password again
        <input type="password" value={repeat} onChange={(e) => setRepeat(e.target.value)} required
               autoComplete="new-password" />
      </label>
      {mismatch && <p className="error">The two new passwords are not the same.</p>}
      <ErrorMessage error={error} />
      <button type="submit" disabled={busy || mismatch}>{busy ? 'Saving…' : 'Save the new password'}</button>
    </form>
  );
}
