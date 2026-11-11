import type { EmailDetails } from '../api/types';

// The email itself: who sent it and how its authentication checks went.
// (Raw headers are left out: they can be very long and are not needed to read the case.)
export default function EmailDetailsSection({ email }: { email: EmailDetails }) {
  return (
    <section className="panel">
      <h2>Email</h2>
      <dl className="fields">
        <dt>Subject</dt><dd>{email.subject}</dd>
        <dt>From</dt><dd>{email.fromAddress}</dd>
        <dt>Envelope sender</dt><dd>{email.envelopeSender ?? '—'}</dd>
        <dt>Reply-To</dt><dd>{email.replyTo ?? '—'}</dd>
        <dt>SPF / DKIM / DMARC</dt>
        <dd>{email.spfResult ?? '—'} / {email.dkimResult ?? '—'} / {email.dmarcResult ?? '—'}</dd>
        <dt>Phish score</dt><dd>{email.phishScore ?? '—'}</dd>
      </dl>
    </section>
  );
}
