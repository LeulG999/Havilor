import { Link } from 'react-router';
import type { AiAssessment } from '../api/types';

// The AI assessment as a simple card (Flow F27; the full design comes later): the suggested verdict and how sure
// the AI is, one line on what happened, the key evidence (most important first, each with how strongly it points to
// a threat), what to do next, what would change its mind, and the reference knowledge it was given.
export default function AssessmentCard({ assessment }: { assessment: AiAssessment }) {
  return (
    <div className="assessment-card">
      <div className="assessment-head">
        <strong>{assessment.suggestedVerdict}</strong>
        <span className={`badge level-${assessment.confidence.toLowerCase()}`}>{assessment.confidence} confidence</span>
      </div>
      <p>{assessment.oneLineSummary}</p>

      <h3>Key evidence</h3>
      {assessment.keyEvidence.length === 0 ? <p className="muted">None given.</p> : (
        <ul className="plain">
          {assessment.keyEvidence.map((e, i) => (
            <li key={i}><span className={`badge level-${e.severity.toLowerCase()}`}>{e.severity}</span> {e.text}</li>
          ))}
        </ul>
      )}

      <h3>Next steps</h3>
      {assessment.nextSteps.length === 0 ? <p className="muted">None given.</p> : (
        <ol>{assessment.nextSteps.map((step, i) => <li key={i}>{step}</li>)}</ol>
      )}

      <p className="muted">Would change my mind: {assessment.wouldChangeMyMindIf}</p>
      {assessment.references.length > 0 && (
        <p className="muted">
          Compared with:{' '}
          {assessment.references.map((r) => r.sourceType === 'CLOSED_ALERT'
            ? <Link key={`a-${r.sourceId}`} to={`/alerts/${r.sourceId}`} className="mention">past alert #{r.sourceId}</Link>
            : <span key={`s-${r.sourceId}`} className="mention">playbook step #{r.sourceId}</span>)}
        </p>
      )}
      <p className="muted">A suggestion only: the verdict is yours when you close the alert.</p>
    </div>
  );
}
