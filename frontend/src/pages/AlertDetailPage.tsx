import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { api } from '../api/api';
import type { AlertDetail, AlertStep, Finding, User } from '../api/types';
import AlertChildrenSection from '../components/AlertChildrenSection';
import AlertHeader from '../components/AlertHeader';
import EmailDetailsSection from '../components/EmailDetailsSection';
import ErrorMessage from '../components/ErrorMessage';
import FindingList from '../components/FindingList';
import OverviewSection from '../components/OverviewSection';
import StepList from '../components/StepList';
import Tabs from '../components/Tabs';

const TABS = ['Overview', 'Investigation', 'Evidence', 'Actions', 'AI'] as const;
type Tab = (typeof TABS)[number];

// One alert: a fixed header, then one tab at a time. Tabs only change what is shown; the data is loaded when the
// page opens and again after each change.
export default function AlertDetailPage() {
  const { id } = useParams(); // the {id} in /alerts/{id}
  const [alert, setAlert] = useState<AlertDetail | null>(null);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [steps, setSteps] = useState<AlertStep[]>([]);
  const [me, setMe] = useState<User | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [tab, setTab] = useState<Tab>('Overview'); // which tab is open: plain React state

  // Loads the alert, its findings and its steps, all at the same time (Promise.all); if any fails, the error is
  // shown. Called when the page opens and again after every change, so the page always shows the backend's state.
  function load() {
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
  }

  useEffect(load, [id]);

  // Who is logged in: needed for "Assign to me" and to show "You" as the assignee.
  useEffect(() => {
    api<User>('/users/me').then(setMe).catch(setError);
  }, []);

  return (
    <main className="page">
      <p><Link to="/alerts">← Back to the queue</Link></p>
      <ErrorMessage error={error} />
      {alert && (
        <>
          <AlertHeader alert={alert} me={me} onChanged={load} />
          <Tabs tabs={[...TABS]} active={tab} onSelect={setTab} />

          {tab === 'Overview' && <OverviewSection alert={alert} />}
          {tab === 'Investigation' && (
            <StepList alertId={alert.id} steps={steps} editable={alert.status !== 'CLOSED'} onChanged={load} />
          )}
          {tab === 'Evidence' && (
            <>
              <EmailDetailsSection email={alert.email} />
              <AlertChildrenSection alert={alert} />
              <FindingList findings={findings} />
            </>
          )}
          {tab === 'Actions' && <section className="panel"><p className="muted">Actions come in Flow F9.</p></section>}
          {tab === 'AI' && <section className="panel"><p className="muted">The AI assistant comes in Flow F10.</p></section>}
        </>
      )}
    </main>
  );
}
