import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { AlertDetail } from '../api/types';
import ErrorMessage from './ErrorMessage';

// The expanded part of a queue row: the facts an analyst checks first, loaded when the row opens
// (GET /api/v1/alerts/{id}): the envelope sender, SPF/DKIM/DMARC, a link on a block list, who clicked, and a link
// to the full alert.
export default function QueueRowDetails({ alertId, columns }: { alertId: number; columns: number }) {
  const [alert, setAlert] = useState<AlertDetail | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api<AlertDetail>(`/alerts/${alertId}`).then(setAlert).catch(setError);
  }, [alertId]);

  return (
    <tr className="row-details">
      <td colSpan={columns}>
        <ErrorMessage error={error} />
        {alert === null ? !error && <span className="faint">Loading…</span> : (
          <div className="kv">
            <div>
              <span className="kv-label">Envelope sender</span>
              <span className="mono">{alert.email.envelopeSender ?? '—'}</span>
            </div>
            <div>
              <span className="kv-label">Authentication</span>
              <span className="auth">
                <AuthTag check="SPF" result={alert.email.spfResult} />
                <AuthTag check="DKIM" result={alert.email.dkimResult} />
                <AuthTag check="DMARC" result={alert.email.dmarcResult} />
              </span>
            </div>
            <div>
              <span className="kv-label">Bad URL</span>
              <BadUrl alert={alert} />
            </div>
            <div>
              <span className="kv-label">Clicked</span>
              <Clicks alert={alert} />
            </div>
            <div className="kv-link">
              <Link to={`/alerts/${alert.id}`}>Open alert →</Link>
            </div>
          </div>
        )}
      </td>
    </tr>
  );
}

// "SPF NONE" / "DKIM FAIL" / "DMARC PASS": green for pass, red for fail, neutral for none or unknown.
export function AuthTag({ check, result }: { check: string; result: string | null }) {
  const tone = result === 'PASS' ? 'pass' : result === 'FAIL' ? 'fail' : 'none';
  return <b className={`auth-${tone}`}>{check} {result ?? '?'}</b>;
}

// The first link that is on a block list (and how many more there are).
function BadUrl({ alert }: { alert: AlertDetail }) {
  const bad = alert.urls.filter((u) => u.onBadList);
  if (bad.length === 0) {
    return <span className="faint">None</span>;
  }
  return (
    <span className="mono bad-url">
      {bad[0].address}{bad.length > 1 && <span className="faint"> +{bad.length - 1} more</span>}
    </span>
  );
}

// The first click: who, allowed or blocked, and how long after the email arrived.
function Clicks({ alert }: { alert: AlertDetail }) {
  if (alert.clicks.length === 0) {
    return <span className="faint">Nobody</span>;
  }
  const first = alert.clicks[0];
  const minutes = Math.round((new Date(first.clickedAt).getTime() - new Date(alert.receivedAt).getTime()) / 60_000);
  return (
    <span>
      {first.recipientAddress.split('@')[0]} · {first.permitted ? 'allowed' : 'blocked'} · {minutes} min after delivery
      {alert.clicks.length > 1 && <span className="faint"> +{alert.clicks.length - 1} more</span>}
    </span>
  );
}
