import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { useAlertData } from '../api/useAlertData';
import ActionList from '../components/ActionList';
import AddActionButton from '../components/AddActionButton';
import AddFindingButton from '../components/AddFindingButton';
import AiAssessmentPanel from '../components/AiAssessmentPanel';
import AlertChildrenSection from '../components/AlertChildrenSection';
import AlertHeader from '../components/AlertHeader';
import AssessmentCard from '../components/AssessmentCard';
import DraftEmailButton from '../components/DraftEmailButton';
import EscalationList from '../components/EscalationList';
import EmailDetailsSection from '../components/EmailDetailsSection';
import ErrorMessage from '../components/ErrorMessage';
import FindingList from '../components/FindingList';
import HistoryTimeline from '../components/HistoryTimeline';
import OverviewSection from '../components/OverviewSection';
import StepList from '../components/StepList';
import Tabs from '../components/Tabs';

const TABS = ['Overview', 'Investigation', 'Evidence', 'Actions', 'AI', 'History'] as const;
type Tab = (typeof TABS)[number];

// One alert: a fixed header, then one tab at a time. Tabs only change what is shown; the data (useAlertData) is
// loaded when the page opens and again after each change (load), so the page always shows the backend's state.
export default function AlertDetailPage() {
  const { id } = useParams(); // the {id} in /alerts/{id}
  const { alert, findings, steps, actions, escalations, history, me, error, reload: load, alertIsOpen, canWork } =
    useAlertData(id);
  const [tab, setTab] = useState<Tab>('Overview'); // which tab is open: plain React state

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
            <StepList alertId={alert.id} steps={steps} editable={canWork} onChanged={load} />
          )}
          {tab === 'Evidence' && (
            <>
              <EmailDetailsSection email={alert.email} />
              <AlertChildrenSection alert={alert} />
              {canWork && <p><AddFindingButton alertId={alert.id} onChanged={load} /></p>}
              <FindingList findings={findings} />
            </>
          )}
          {tab === 'Actions' && (
            <>
              {canWork && <p><AddActionButton alertId={alert.id} onChanged={load} /></p>}
              <ActionList actions={actions} />
              <EscalationList escalations={escalations} editable={canWork} onChanged={load} />
            </>
          )}
          {tab === 'AI' && (canWork ? (
            <>
              {/* key: a new alert (e.g. after clicking a reference) starts with an empty panel */}
              <AiAssessmentPanel key={alert.id} alertId={alert.id} saved={alert.lastAssessment} onChanged={load} />
              <p><DraftEmailButton alertId={alert.id} /></p>
            </>
          ) : (
            <section className="panel">
              <p className="muted">
                {alertIsOpen ? 'The AI assistant is for the analyst working the alert.' : 'This alert is closed.'}
              </p>
              {alert.lastAssessment ? <AssessmentCard assessment={alert.lastAssessment} />
                : <p className="muted">No AI assessment was made.</p>}
            </section>
          ))}
          {tab === 'History' && <HistoryTimeline entries={history} me={me} />}
        </>
      )}
    </main>
  );
}
