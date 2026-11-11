import { useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { AiAssessment } from '../api/types';
import ErrorMessage from './ErrorMessage';

// "Get AI assessment" → POST /api/v1/alerts/{alertId}/ai/assessment. Claude reads the alert, its findings and similar
// past cases, and suggests a verdict. The suggestion is saved on the alert (so the page reloads); the analyst still
// decides. If the AI is not available, the backend answers 502 and its message is shown.
export default function AiAssessmentPanel({ alertId, onChanged }: { alertId: number; onChanged: () => void }) {
  const [assessment, setAssessment] = useState<AiAssessment | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function assess() {
    setLoading(true);
    setError(null);
    try {
      setAssessment(await api<AiAssessment>(`/alerts/${alertId}/ai/assessment`, { method: 'POST' }));
      onChanged(); // the suggested verdict + reason now show on the Overview tab too
    } catch (e) {
      setError(e); // e.g. 502 "The AI assistant is not configured (ANTHROPIC_API_KEY is not set)"
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="panel">
      <h2>AI assessment</h2>
      <button type="button" disabled={loading} onClick={assess}>
        {loading ? 'Asking the AI… (this can take a little while)' : 'Get AI assessment'}
      </button>
      <ErrorMessage error={error} />
      {assessment && (
        <div className="ai-result">
          <p>{assessment.summary}</p>
          <dl className="fields">
            <dt>Suggested verdict</dt><dd><strong>{assessment.suggestedVerdict}</strong></dd>
            <dt>Reason</dt><dd>{assessment.suggestionReason}</dd>
            <dt>Reference knowledge</dt>
            <dd>
              {assessment.references.length === 0 ? <span className="muted">None used.</span> : (
                <ul className="plain">
                  {assessment.references.map((r) => (
                    <li key={`${r.sourceType}-${r.sourceId}`}>
                      {r.sourceType === 'CLOSED_ALERT'
                        ? <Link to={`/alerts/${r.sourceId}`}>Past alert #{r.sourceId}</Link>
                        : `Playbook step #${r.sourceId}`}
                    </li>
                  ))}
                </ul>
              )}
            </dd>
          </dl>
          <p className="muted">A suggestion only: the verdict is yours when you close the alert.</p>
        </div>
      )}
    </section>
  );
}
