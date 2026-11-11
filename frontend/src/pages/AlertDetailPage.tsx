import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { api } from '../api/api';
import type { Action, AlertDetail, AlertStep, Finding, User } from '../api/types';
import ActionList from '../components/ActionList';
import AddActionButton from '../components/AddActionButton';
import AddFindingButton from '../components/AddFindingButton';
import AiAssessmentPanel from '../components/AiAssessmentPanel';
import AlertChildrenSection from '../components/AlertChildrenSection';
import AlertHeader from '../components/AlertHeader';
import DraftEmailButton from '../components/DraftEmailButton';
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
  const [actions, setActions] = useState<Action[]>([]);
  const [me, setMe] = useState<User | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [tab, setTab] = useState<Tab>('Overview'); // which tab is open: plain React state

  // Loads the alert, its findings, steps and actions, all at the same time (Promise.all); if any fails, the error is
  // shown. Called when the page opens and again after every change, so the page always shows the backend's state.
  function load() {
    setError(null);
    Promise.all([
      api<AlertDetail>(`/alerts/${id}`),
      api<Finding[]>(`/alerts/${id}/findings`),
      api<AlertStep[]>(`/alerts/${id}/steps`),
      api<Action[]>(`/alerts/${id}/actions`),
    ])
      .then(([alertAnswer, findingsAnswer, stepsAnswer, actionsAnswer]) => {
        setAlert(alertAnswer);
        setFindings(findingsAnswer);
        setSteps(stepsAnswer);
        setActions(actionsAnswer);
      })
      .catch(setError);
  }

  useEffect(load, [id]);

  // Who is logged in: needed for "Assign to me" and to show "You" as the assignee.
  useEffect(() => {
    api<User>('/users/me').then(setMe).catch(setError);
  }, []);

  // A closed alert is final: no buttons or forms that would change it.
  const alertIsOpen = alert !== null && alert.status !== 'CLOSED';

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
            <StepList alertId={alert.id} steps={steps} editable={alertIsOpen} onChanged={load} />
          )}
          {tab === 'Evidence' && (
            <>
              <EmailDetailsSection email={alert.email} />
              <AlertChildrenSection alert={alert} />
              {alertIsOpen && <p><AddFindingButton alertId={alert.id} onChanged={load} /></p>}
              <FindingList findings={findings} />
            </>
          )}
          {tab === 'Actions' && (
            <>
              {alertIsOpen && <p><AddActionButton alertId={alert.id} onChanged={load} /></p>}
              <ActionList actions={actions} />
            </>
          )}
          {tab === 'AI' && (alertIsOpen ? (
            <>
              {/* key: a new alert (e.g. after clicking a reference) starts with an empty panel */}
              <AiAssessmentPanel key={alert.id} alertId={alert.id} onChanged={load} />
              <p><DraftEmailButton alertId={alert.id} /></p>
            </>
          ) : (
            <section className="panel">
              <p className="muted">This alert is closed. Its saved AI suggestion is on the Overview tab.</p>
            </section>
          ))}
        </>
      )}
    </main>
  );
}
