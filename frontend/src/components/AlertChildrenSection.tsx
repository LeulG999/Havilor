import type { AlertDetail } from '../api/types';
import { formatDateTime } from '../format';

// What came with the email: who got it, the links in it, the attached files, and who clicked what.
export default function AlertChildrenSection({ alert }: { alert: AlertDetail }) {
  return (
    <section className="panel">
      <h2>Recipients</h2>
      <table>
        <thead><tr><th>Address</th><th>Delivery</th></tr></thead>
        <tbody>
          {alert.recipients.map((r) => (
            <tr key={r.id}><td>{r.address}</td><td>{r.delivery}</td></tr>
          ))}
        </tbody>
      </table>

      <h2>URLs</h2>
      {alert.urls.length === 0 ? <p className="muted">None.</p> : (
        <table>
          <thead><tr><th>Address</th><th>Domain</th><th>On a bad list</th></tr></thead>
          <tbody>
            {alert.urls.map((u) => (
              <tr key={u.id}>
                <td className="wrap">{u.address}</td>
                <td>{u.domain ?? '—'}</td>
                <td>{u.onBadList ? <strong className="error">Yes</strong> : 'No'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Attachments</h2>
      {alert.attachments.length === 0 ? <p className="muted">None.</p> : (
        <table>
          <thead><tr><th>File</th><th>Type</th><th>SHA-256</th><th>Verdict</th></tr></thead>
          <tbody>
            {alert.attachments.map((a) => (
              <tr key={a.id}>
                <td>{a.fileName}</td>
                <td>{a.fileType ?? '—'}</td>
                <td className="wrap">{a.sha256 ?? '—'}</td>
                <td>{a.verdict ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Clicks</h2>
      {alert.clicks.length === 0 ? <p className="muted">Nobody clicked.</p> : (
        <table>
          <thead><tr><th>Recipient</th><th>URL</th><th>When</th><th>Let through</th></tr></thead>
          <tbody>
            {alert.clicks.map((c) => (
              <tr key={c.id}>
                <td>{c.recipientAddress}</td>
                <td className="wrap">{c.urlAddress}</td>
                <td>{formatDateTime(c.clickedAt)}</td>
                <td>{c.permitted ? 'Yes' : 'No (blocked)'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
