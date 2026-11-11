import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { api } from '../api/api';
import type { AlertDetail, AlertStep, Finding } from '../api/types';
import AlertChildrenSection from '../components/AlertChildrenSection';
import EmailDetailsSection from '../components/EmailDetailsSection';
import ErrorMessage from '../components/ErrorMessage';
import FindingList from '../components/FindingList';
import StepList from '../components/StepList';
import { formatDateTime } from '../format';

// One alert, read only: the alert itself, its findings and its playbook steps.
export default function AlertDetailPage() {
  const { id } = useParams(); // the {id} in /alerts/{id}
  const [alert, setAlert] = useState<AlertDetail | null>(null);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [steps, setSteps] = useState<AlertStep[]>([]);
  const [error, setError] = useState<unknown>(null);

  // Three requests at the same time (Promise.all); if any fails, its error is shown.
  useEffect(() => {
    setError(null);
    Promise.all([
      api<AlertDetail>(`/alerts/${id}`),
      api<Finding[]>(`/alerts/${id}/findings`),
      api<AlertStep[]>(`/alerts/${id}/steps`),
    ])
      .then(([alertAnswer, findingsAnswer, stepsAnswer]) => {
        setAlert(alertAnswer);
        setFindings(findingsAnswer);
        setSteps(stepsAnswer);
      })
      .catch(setError);
  }, [id]);

  return (
    <main className="page">
      <p><Link to="/alerts">← Back to the queue</Link></p>
      <ErrorMessage error={error} />
      {alert && (
        <>
          <h1>{alert.ticketNumber}: {alert.email.subject}</h1>

          <section className="panel">
            <dl className="fields">
              <dt>Status</dt><dd>{alert.status}</dd>
              <dt>Priority</dt>
              <dd><span className={`badge priority-${alert.priority.toLowerCase()}`}>{alert.priority}</span></dd>
              <dt>Verdict</dt><dd>{alert.verdict}</dd>
              <dt>Suggested verdict</dt><dd>{alert.suggestedVerdict ?? '—'}</dd>
              <dt>Reason</dt><dd>{alert.suggestionReason ?? '—'}</dd>
              <dt>Case type / source</dt><dd>{alert.caseType} / {alert.source}</dd>
              <dt>Received</dt><dd>{formatDateTime(alert.receivedAt)}</dd>
              <dt>Closed</dt><dd>{formatDateTime(alert.closedAt)}</dd>
            </dl>
          </section>

          <EmailDetailsSection email={alert.email} />
          <FindingList findings={findings} />
          <StepList steps={steps} />
          <AlertChildrenSection alert={alert} />
        </>
      )}
    </main>
  );
}
