import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import type { CreatedUser } from '../api/types';
import ErrorMessage from './ErrorMessage';
import Modal from './Modal';

// "Add person" (managers): POST /api/v1/users {name, email, role}. Havilor makes a temporary password; this is the
// ONE moment it can be seen (only its hash is stored), so the modal shows it with a Copy button and says so clearly.
// The person must replace it at their first login.
export default function AddPersonButton({ onAdded }: { onAdded: () => void }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('ANALYST');
  const [created, setCreated] = useState<CreatedUser | null>(null);
  const [copied, setCopied] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  function openModal() {
    setName('');
    setEmail('');
    setRole('ANALYST');
    setCreated(null);
    setCopied(false);
    setError(null);
    setOpen(true);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      setCreated(await api<CreatedUser>('/users', { method: 'POST', body: { name, email, role } }));
      onAdded(); // the list behind the modal shows the new person
    } catch (e) {
      setError(e); // e.g. 409 "A user with the email … already exists"
    } finally {
      setBusy(false);
    }
  }

  async function copy() {
    if (created) {
      await navigator.clipboard.writeText(created.temporaryPassword);
      setCopied(true);
    }
  }

  return (
    <>
      <button type="button" onClick={openModal}>+ Add person</button>
      {/* Closing forgets the password: it is only kept in this modal's state. */}
      <Modal open={open} title={created ? 'Person added' : 'Add person'} onClose={() => { setOpen(false); setCreated(null); }}>
        {created ? (
          <div className="temp-password">
            <p>
              <b>{created.user.name}</b> ({created.user.email}) can now log in with this temporary password and must
              choose their own at the first login.
            </p>
            <div className="temp-password-box">
              <code className="mono">{created.temporaryPassword}</code>
              <button type="button" className="secondary small" onClick={copy}>{copied ? 'Copied ✓' : 'Copy'}</button>
            </div>
            <p className="temp-password-note">
              You won't see this password again. Give it to {created.user.name} in a safe way (not in the same email as
              the login address).
            </p>
            <button type="button" onClick={() => { setOpen(false); setCreated(null); }}>Done</button>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <label>
              Name
              <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
            </label>
            <label>
              Work email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={320} />
            </label>
            <label>
              Role
              <select value={role} onChange={(e) => setRole(e.target.value)}>
                <option value="ANALYST">Analyst</option>
                <option value="MANAGER">Manager</option>
              </select>
            </label>
            <ErrorMessage error={error} />
            <button type="submit" disabled={busy}>{busy ? 'Adding…' : 'Add person'}</button>
          </form>
        )}
      </Modal>
    </>
  );
}
