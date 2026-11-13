import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { api, saveToken } from '../api/api';
import type { LoginResponse } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';
import LoginBrandPanel from '../components/LoginBrandPanel';

// The login page (docs/design/mockups/login-white.png): the dark brand panel on the left, a white sign-in panel on
// the right. POST /api/v1/auth/login → keep the token → go to the dashboard.
export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
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
    <div className="login-page">
      <LoginBrandPanel />
      <main className="login-side">
        <form className="login-card" onSubmit={handleSubmit}>
          <h2>Sign in</h2>
          <p className="login-sub">Welcome back. Use your Havilor work account.</p>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus
                   autoComplete="username" />
          </label>
          <label>
            Password
            {/* "Show" switches the input between hidden dots and plain text. */}
            <span className="password-field">
              <input type={showPassword ? 'text' : 'password'} value={password}
                     onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
              <button type="button" className="show-password" onClick={() => setShowPassword(!showPassword)}
                      aria-label={showPassword ? 'Hide the password' : 'Show the password'}>
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </span>
          </label>
          <ErrorMessage error={error} />
          <button type="submit" className="login-button" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
          <div className="login-note">
            <span className="login-note-icon" aria-hidden="true">ⓘ</span>
            <span><b>Accounts are created by your admin.</b> Ask your SOC lead if you can't sign in.</span>
          </div>
        </form>
      </main>
    </div>
  );
}
