import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { api, saveToken } from '../api/api';
import type { LoginResponse } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';

// POST /api/v1/auth/login → keep the token → go to the dashboard.
export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault(); // stop the browser from reloading the page; we send the form ourselves
    setBusy(true);
    setError(null);
    try {
      const login = await api<LoginResponse>('/auth/login', { method: 'POST', body: { email, password } });
      saveToken(login.accessToken);
      navigate('/dashboard');
    } catch (e) {
      setError(e); // e.g. "Invalid email or password", straight from the backend
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="page login">
      <h1>Havilor</h1>
      <form className="panel" onSubmit={handleSubmit}>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <ErrorMessage error={error} />
        <button type="submit" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
      </form>
    </main>
  );
}
