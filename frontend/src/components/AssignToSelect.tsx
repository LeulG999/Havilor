import { useEffect, useState } from 'react';
import { api } from '../api/api';
import type { User } from '../api/types';
import ErrorMessage from './ErrorMessage';

// For a MANAGER: "Assign to…" with the active analysts (GET /api/v1/users?role=ANALYST, managers only).
// Choosing one tells the page, which sends PATCH /api/v1/alerts/{id} {assigneeId}. The backend checks again that
// the person is an active analyst.
export default function AssignToSelect({ assigneeId, disabled, onAssign }: {
  assigneeId: number | null;
  disabled: boolean;
  onAssign: (analystId: number) => void;
}) {
  const [analysts, setAnalysts] = useState<User[]>([]);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api<User[]>('/users?role=ANALYST').then(setAnalysts).catch(setError);
  }, []);

  return (
    <label className="inline">
      Assign to
      <select value={assigneeId ?? ''} disabled={disabled}
              onChange={(e) => e.target.value !== '' && onAssign(Number(e.target.value))}>
        <option value="" disabled>Choose…</option>
        {analysts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
      </select>
      <ErrorMessage error={error} />
    </label>
  );
}
