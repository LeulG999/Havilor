import { useState } from 'react';
import { useParams } from 'react-router';
import { useAlertData } from '../api/useAlertData';
import ActionList from '../components/ActionList';
import AddActionButton from '../components/AddActionButton';
import AddFindingButton from '../components/AddFindingButton';
import AiAssessmentPanel from '../components/AiAssessmentPanel';
import AlertCards from '../components/AlertCards';
import AlertChildrenSection from '../components/AlertChildrenSection';
import AssessmentCard from '../components/AssessmentCard';
import CloseAlertButton from '../components/CloseAlertButton';
import DraftEmailButton from '../components/DraftEmailButton';
import EmailCard from '../components/EmailCard';
import EmailDetailsSection from '../components/EmailDetailsSection';
import ErrorMessage from '../components/ErrorMessage';
import EscalateButton from '../components/EscalateButton';
import EscalationList from '../components/EscalationList';
import FindingList from '../components/FindingList';
import HistoryTimeline from '../components/HistoryTimeline';
import { PriorityTag, StatusChip } from '../components/QueueBits';
import StepList from '../components/StepList';
import { statusLabel } from '../format';

type Tab = 'Overview' | 'Investigation' | 'Evidence' | 'Actions' | 'AI' | 'History';

// How many history entries the Overview's side column shows (the newest); "All n ›" opens the History tab.
const SIDE_HISTORY = 7;

// One alert (docs/design/mockups/s3-detail.png): ticket, title and a line of facts; the alert-wide buttons on the
// right; the summary cards (status, assignee, priority, verdict); then one tab at a time. The Overview puts the AI
// card and the email on the left and the latest history on the right. The data (useAlertData) is loaded when the
// page opens and again after each change, so the page always shows the backend's state.
export default function AlertDetailPage() {
  const { id } = useParams(); // the {id} in /alerts/{id}
  const { alert, findings, steps, actions, escalations, history, me, error, reload: load, alertIsOpen, canWork } =
    useAlertData(id);
  const [tab, setTab] = useState<Tab>('Overview'); // which tab is open: plain React state

  const done = steps.filter((s) => s.status !== 'PENDING').length;
  const tabs: { name: Tab; count?: string }[] = [
    { name: 'Overview' },
    { name: 'Investigation', count: `${done}/${steps.length}` },
    { name: 'Evidence', count: String(findings.length) },
    { name: 'Actions', count: String(actions.length) },
    { name: 'AI' },
    { name: 'History', count: String(history.length) },
  ];

  return (
    <main className="page">
      <ErrorMessage error={error} />
      {alert && (
        <>
          <div className="detail-head">
            <div>
              <span className="mono muted">{alert.ticketNumber}</span>
              <h1 className="detail-title">{alert.email.subject}</h1>
              <div className="meta">
                <StatusChip status={alert.status} />
                <PriorityTag priority={alert.priority} />
                <span className="sep" />
                <span>{statusLabel(alert.caseType)}</span>
                <span className="sep" />
                <span>Source: {statusLabel(alert.source).toLowerCase()}</span>
                <span className="sep" />
                <span className="mono">
                  {new Date(alert.receivedAt).toLocaleString([], {
                    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
                  })}
                </span>
              </div>
            </div>
            {/* The investigation buttons are the analyst's, on an open alert (the backend enforces it too). */}
            {canWork && (
              <div className="buttons detail-actions">
                <EscalateButton alertId={alert.id} onChanged={load} />
                <DraftEmailButton alertId={alert.id} />
                <CloseAlertButton alertId={alert.id} onChanged={load} />
              </div>
            )}
          </div>

          <AlertCards alert={alert} me={me} withPriority onChanged={load} />

          <nav className="tabs" role="tablist">
            {tabs.map((t) => (
              <button key={t.name} type="button" role="tab" aria-selected={tab === t.name}
                      className={tab === t.name ? 'tab active' : 'tab'} onClick={() => setTab(t.name)}>
                {t.name} {t.count !== undefined && <em>{t.count}</em>}
              </button>
            ))}
          </nav>

          {tab === 'Overview' && (
            <div className="overview-grid">
              <div className="overview-main">
                {alert.lastAssessment ? <AssessmentCard assessment={alert.lastAssessment} /> : (
                  <section className="sec sec-padded">
                    <b>AI assessment</b>
                    <p className="muted">
                      No assessment yet.{' '}
                      {canWork && <button type="button" className="link" onClick={() => setTab('AI')}>Ask the AI ›</button>}
                    </p>
                  </section>
                )}
                <EmailCard alert={alert} onShowEvidence={() => setTab('Evidence')} />
              </div>
              <section className="sec side-history">
                <div className="sec-head">
                  <b>History</b>
                  <button type="button" className="link quiet" onClick={() => setTab('History')}>
                    All {history.length} ›
                  </button>
                </div>
                {/* The newest entries first in the side column. */}
                <HistoryTimeline entries={history.slice(-SIDE_HISTORY).reverse()} me={me} />
              </section>
            </div>
          )}
          {tab === 'Investigation' && <StepList alertId={alert.id} steps={steps} editable={canWork} onChanged={load} />}
          {tab === 'Evidence' && (
            <>
              {canWork && <p><AddFindingButton alertId={alert.id} onChanged={load} /></p>}
              <FindingList findings={findings} />
              <EmailDetailsSection email={alert.email} />
              <AlertChildrenSection alert={alert} />
            </>
          )}
          {tab === 'Actions' && (
            <>
              {canWork && <p><AddActionButton alertId={alert.id} onChanged={load} /></p>}
              <ActionList actions={actions} />
              <EscalationList escalations={escalations} editable={canWork} onChanged={load} />
            </>
          )}
          {tab === 'AI' && (canWork
            ? <AiAssessmentPanel key={alert.id} alertId={alert.id} saved={alert.lastAssessment} onChanged={load} />
            : (
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
