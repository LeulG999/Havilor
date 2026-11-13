import { useNavigate } from 'react-router';
import { clearToken } from '../api/api';
import type { User } from '../api/types';
import ChangePasswordForm from '../components/ChangePasswordForm';
import LoginBrandPanel from '../components/LoginBrandPanel';

// The first-login screen: someone a manager created logs in with a temporary password. Until they set their own,
// this is the only page they see (and the backend refuses everything else with 403 "Set a new password first").
// Same look as the login page: the brand panel left, the white panel right.
export default function SetPasswordPage({ me, onDone }: { me: User; onDone: () => void }) {
  const navigate = useNavigate();

  function logout() {
    clearToken();
    navigate('/login');
  }

  return (
    <div className="login-page">
      <LoginBrandPanel />
      <main className="login-side">
        <div className="login-card">
          <h2>Set your password</h2>
          <p className="login-sub">Welcome, {me.name}. Your account has a temporary password: choose your own to start.</p>
          <ChangePasswordForm currentLabel="Temporary password" onDone={onDone} />
          <p className="login-sub small-print">
            Not you? <button type="button" className="link" onClick={logout}>Log out</button>
          </p>
        </div>
      </main>
    </div>
  );
}
