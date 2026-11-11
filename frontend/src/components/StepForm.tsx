import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import type { AlertStep } from '../api/types';
import ErrorMessage from './ErrorMessage';

// The answer form under one PENDING step: pick an answer (if the step offers options) or type one,
// then mark the step DONE or SKIPPED → PATCH /api/v1/alerts/{alertId}/steps/{stepId}.
export default function StepForm({ alertId, step, onChanged }: {
  alertId: number;
  step: AlertStep;
  onChanged: () => void;
}) {
  const [answer, setAnswer] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function record(status: 'DONE' | 'SKIPPED') {
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alertId}/steps/${step.stepId}`, { method: 'PATCH', body: { status, answer } });
      onChanged();
    } catch (e) {
      setError(e);
      setBusy(false); // on success the form disappears (the step is no longer PENDING), so only reset on failure
    }
  }

  // Enter in the text box means "Done".
  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    record('DONE');
  }

  return (
    <form className="step-form" onSubmit={handleSubmit}>
      {step.answerOptions.length > 0 ? (
        <select value={answer} onChange={(e) => setAnswer(e.target.value)} aria-label="Answer">
          <option value="">— choose an answer —</option>
          {step.answerOptions.map((option) => <option key={option} value={option}>{option}</option>)}
        </select>
      ) : (
        <textarea value={answer} onChange={(e) => setAnswer(e.target.value)} rows={2} maxLength={2000}
                  placeholder="Your answer" aria-label="Answer" />
      )}
      <div className="buttons">
        <button type="submit" disabled={busy}>Done</button>
        <button type="button" className="secondary" disabled={busy} onClick={() => record('SKIPPED')}>Skip</button>
      </div>
      <ErrorMessage error={error} />
    </form>
  );
}
