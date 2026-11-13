import { useState } from 'react';
import type { AlertDetail } from '../api/types';
import { AuthTag } from './QueueRowDetails';

// The "Email" section of the detail page's Overview (docs/design/mockups/s3-detail.png): from, envelope sender,
// SPF/DKIM/DMARC, recipients and phish score. "Less detail ⌃" folds it to its header; "More detail ⌄" opens it again.
// "view" on the recipients jumps to the Evidence tab, where every recipient, URL, attachment and click is listed.
export default function EmailCard({ alert, onShowEvidence }: { alert: AlertDetail; onShowEvidence: () => void }) {
  const [open, setOpen] = useState(true);
  const { email } = alert;
  const delivered = alert.recipients.filter((r) => r.delivery === 'DELIVERED').length;

  return (
    <section className="sec">
      <div className="sec-head">
        <b>Email</b>
        <button type="button" className="link quiet" aria-expanded={open} onClick={() => setOpen(!open)}>
          {open ? 'Less detail ⌃' : 'More detail ⌄'}
        </button>
      </div>
      {open && (
        <dl className="props">
          <dt>From</dt><dd className="mono">{email.fromAddress}</dd>
          <dt>Envelope sender</dt><dd className="mono">{email.envelopeSender ?? '—'}</dd>
          {email.replyTo && <><dt>Reply-To</dt><dd className="mono">{email.replyTo}</dd></>}
          <dt>Authentication</dt>
          <dd className="auth">
            <AuthTag check="SPF" result={email.spfResult} />
            <AuthTag check="DKIM" result={email.dkimResult} />
            <AuthTag check="DMARC" result={email.dmarcResult} />
          </dd>
          <dt>Recipients</dt>
          <dd>
            {alert.recipients.length} recipient{alert.recipients.length === 1 ? '' : 's'}, {delivered} delivered ·{' '}
            <button type="button" className="link" onClick={onShowEvidence}>view</button>
          </dd>
          <dt>Phish score</dt>
          <dd className={email.phishScore !== null && email.phishScore >= 70 ? 'mono danger-text' : 'mono'}>
            {email.phishScore === null ? '—' : `${email.phishScore} / 100`}
          </dd>
        </dl>
      )}
    </section>
  );
}
