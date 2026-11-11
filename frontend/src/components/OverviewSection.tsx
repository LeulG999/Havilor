import type { AlertDetail } from '../api/types';
import { formatDateTime } from '../format';

// The Overview tab: the verdicts and the key facts of the case.
export default function OverviewSection({ alert }: { alert: AlertDetail }) {
  return (
    <section className="panel">
      <dl className="fields">
        <dt>Verdict</dt><dd>{alert.verdict}</dd>
        <dt>Suggested verdict</dt><dd>{alert.suggestedVerdict ?? '—'}</dd>
        <dt>Reason</dt><dd>{alert.suggestionReason ?? '—'}</dd>
        <dt>From</dt><dd>{alert.email.fromAddress}</dd>
        <dt>Case type / source</dt><dd>{alert.caseType} / {alert.source}</dd>
        <dt>Received</dt><dd>{formatDateTime(alert.receivedAt)}</dd>
        <dt>Closed</dt><dd>{formatDateTime(alert.closedAt)}</dd>
      </dl>
    </section>
  );
}
