import { Link } from 'react-router';
import type { AiAssessment } from '../api/types';

const LEVELS = ['LOW', 'MEDIUM', 'HIGH'];

// The AI assessment card (docs/design/mockups/s2-drawer.png): cyan border with a faint glow at the top, the
// suggested verdict as a badge, a 3-step confidence meter, one line on what happened, the key evidence (a dot per
// item, coloured by how strongly it points to a threat), the next steps as a checklist (display only), what would
// change the AI's mind, and the reference knowledge it was given. A suggestion only: the analyst decides.
export default function AssessmentCard({ assessment }: { assessment: AiAssessment }) {
  const filled = LEVELS.indexOf(assessment.confidence) + 1; // LOW = 1 bar, MEDIUM = 2, HIGH = 3

  return (
    <div className="ai-card">
      <div className="ai-card-head">
        <span className="ai-card-title"><span className="ai-spark" aria-hidden="true">✦</span> AI assessment</span>
        <span className={`verdict verdict-${assessment.suggestedVerdict.toLowerCase()}`}>
          {assessment.suggestedVerdict.replace('_', ' ')}
        </span>
        <span className="confidence" aria-label={`Confidence: ${assessment.confidence}`}>
          Confidence
          {LEVELS.map((level, i) => <i key={level} className={i < filled ? 'on' : ''} aria-hidden="true" />)}
          {assessment.confidence.charAt(0) + assessment.confidence.slice(1).toLowerCase()}
        </span>
      </div>

      <p className="ai-card-one">{assessment.oneLineSummary}</p>

      <h4>Key evidence</h4>
      {assessment.keyEvidence.length === 0 ? <p className="ai-card-none">None given.</p> : (
        <ul className="evidence">
          {assessment.keyEvidence.map((e, i) => (
            <li key={i}>
              <i className={`dot level-dot-${e.severity.toLowerCase()}`} aria-label={`${e.severity} severity`} />
              <span>{e.text}</span>
            </li>
          ))}
        </ul>
      )}

      <h4>Next steps</h4>
      {assessment.nextSteps.length === 0 ? <p className="ai-card-none">None given.</p> : (
        <ul className="next-steps">
          {assessment.nextSteps.map((step, i) => <li key={i}><i className="box" aria-hidden="true" />{step}</li>)}
        </ul>
      )}

      <div className="ai-card-foot">
        <span>Would change my mind if {lowerFirst(assessment.wouldChangeMyMindIf)}</span>
        {assessment.references.length > 0 && (
          <span className="ai-card-refs">
            Compared with:{' '}
            {assessment.references.map((r) => r.sourceType === 'CLOSED_ALERT'
              ? <Link key={`a-${r.sourceId}`} to={`/alerts/${r.sourceId}`} className="mention">alert #{r.sourceId}</Link>
              : <span key={`s-${r.sourceId}`} className="mention">playbook step #{r.sourceId}</span>)}
          </span>
        )}
      </div>
    </div>
  );
}

// "The vendor confirms…" → "the vendor confirms…", to read on after "Would change my mind if".
function lowerFirst(text: string): string {
  return text.charAt(0).toLowerCase() + text.slice(1);
}
