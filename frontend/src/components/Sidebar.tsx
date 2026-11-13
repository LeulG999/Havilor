import { useState, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router';
import { clearToken } from '../api/api';
import type { User } from '../api/types';
import Avatar from './Avatar';
import ChangePasswordForm from './ChangePasswordForm';
import { AuditIcon, DashboardIcon, KeyIcon, LogoutIcon, NoteIcon, PeopleIcon, PlusIcon, QueueIcon, TeamIcon } from './icons';
import Modal from './Modal';

// The 64px icon rail on the left of every logged-in page (docs/design/mockups/s1-queue.png): the logo, one icon per
// page (the name shows as a tooltip on hover or keyboard focus), the manager pages only for a MANAGER (from
// GET /api/v1/users/me, loaded by LoggedInLayout; the backend refuses them for anyone else anyway), and at the bottom
// the user's avatar, which opens a small menu with the name, role, "Change password" and "Log out".
export default function Sidebar({ user }: { user: User | null }) {
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordChanged, setPasswordChanged] = useState(false);

  function logout() {
    clearToken(); // the token is only in this tab's sessionStorage, so forgetting it logs out
    navigate('/login');
  }

  return (
    <aside className="rail">
      <img className="rail-logo" src="/havilor-mark-white.svg" alt="Havilor" width="30" height="30" />
      <nav className="rail-nav" aria-label="Main">
        <RailLink to="/dashboard" label="Dashboard" icon={<DashboardIcon />} />
        {/* `end`: only exactly /alerts counts, not /alerts/new or /alerts/42. */}
        <RailLink to="/alerts" end label="Alert queue" icon={<QueueIcon />} />
        <RailLink to="/alerts/new" label="New alert" icon={<PlusIcon />} />
        <RailLink to="/standup" label="Standup notes" icon={<NoteIcon />} />
        {user?.role === 'MANAGER' && (
          <>
            <span className="rail-divider" aria-hidden="true" />
            <RailLink to="/team-standups" label="Team standups" icon={<TeamIcon />} />
            <RailLink to="/audit" label="Audit log" icon={<AuditIcon />} />
            <RailLink to="/team" label="Team" icon={<PeopleIcon />} />
          </>
        )}
      </nav>

      <div className="rail-bottom">
        {user && (
          <>
            <button type="button" className="rail-avatar" onClick={() => setMenuOpen(!menuOpen)}
                    aria-expanded={menuOpen} aria-label={`${user.name}: account menu`}>
              <Avatar name={user.name} />
            </button>
            {menuOpen && (
              <div className="rail-menu" role="menu">
                <div className="rail-menu-name">{user.name}</div>
                <div className="rail-menu-role">{user.role}</div>
                <div className="rail-menu-buttons">
                  <button type="button" className="secondary small" role="menuitem"
                          onClick={() => { setMenuOpen(false); setPasswordChanged(false); setPasswordOpen(true); }}>
                    <KeyIcon size={14} /> Change password
                  </button>
                  <button type="button" className="secondary small" role="menuitem" onClick={logout}>
                    <LogoutIcon size={14} /> Log out
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <Modal open={passwordOpen} title="Change password" onClose={() => setPasswordOpen(false)}>
        {passwordChanged
          ? <p className="muted">Your password was changed. Use it the next time you log in.</p>
          : <ChangePasswordForm currentLabel="Current password" onDone={() => setPasswordChanged(true)} />}
      </Modal>
    </aside>
  );
}

// One rail icon. NavLink knows whether its link is the current page (isActive) → the cyan "on" style.
// data-tip is shown as a tooltip by CSS; aria-label gives the name to screen readers.
function RailLink({ to, end, label, icon }: { to: string; end?: boolean; label: string; icon: ReactNode }) {
  return (
    <NavLink to={to} end={end} className={({ isActive }) => (isActive ? 'rail-link active' : 'rail-link')}
             aria-label={label} data-tip={label}>
      {icon}
    </NavLink>
  );
}
