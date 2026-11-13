import { useState } from 'react';
import { api } from '../api/api';
import type { AiAssessment } from '../api/types';
import AssessmentCard from './AssessmentCard';
import ErrorMessage from './ErrorMessage';

// "Get AI assessment" → POST /api/v1/alerts/{alertId}/ai/assessment. Claude reads the alert, its findings and similar
// past cases, and suggests a verdict. The whole assessment is saved on the alert (lastAssessment), so the card shows
// the saved one until a new one is asked for; the page reloads after each. The analyst still decides.
// If the AI is not available, the backend answers 502 and its message is shown.
export default function AiAssessmentPanel({ alertId, saved, onChanged }: {
  alertId: number;
  saved: AiAssessment | null;
  onChanged: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function assess() {
    setLoading(true);
    setError(null);
    try {
      await api<AiAssessment>(`/alerts/${alertId}/ai/assessment`, { method: 'POST' });
      onChanged(); // reload: the new assessment comes back as the alert's lastAssessment
    } catch (e) {
      setError(e); // e.g. 502 "The AI assistant is not configured (ANTHROPIC_API_KEY is not set)"
    } finally {
      setLoading(false);
    }
  }

  const label = loading ? 'Asking the AI… (this can take a little while)' : saved ? 'Assess again' : 'Get AI assessment';

  return (
    <div className="ai-panel">
      {saved ? <AssessmentCard assessment={saved} /> : (
        <section className="panel">
          <h2>AI assessment</h2>
          <p className="muted">
            The AI reads this alert, its findings and similar past cases, and suggests a verdict with the evidence
            behind it. You make the call.
          </p>
        </section>
      )}
      <div className="buttons ai-panel-actions">
        {/* Once there is an assessment, asking again is a secondary action. */}
        <button type="button" className={saved ? 'secondary' : ''} disabled={loading} onClick={assess}>{label}</button>
      </div>
      <ErrorMessage error={error} />
    </div>
  );
}
