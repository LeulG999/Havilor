import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { useAlertData } from '../api/useAlertData';
import ActionList from './ActionList';
import AddActionButton from './AddActionButton';
import AddFindingButton from './AddFindingButton';
import AiAssessmentPanel from './AiAssessmentPanel';
import AlertChildrenSection from './AlertChildrenSection';
import AssessmentCard from './AssessmentCard';
import AlertActionButtons from './AlertActionButtons';
import AlertCards from './AlertCards';
import EmailDetailsSection from './EmailDetailsSection';
import ErrorMessage from './ErrorMessage';
import EscalationList from './EscalationList';
import FindingList from './FindingList';
import HistoryTimeline from './HistoryTimeline';
import { PriorityTag, StatusChip } from './QueueBits';
import StepList from './StepList';
import { statusLabel } from '../format';

type Tab = 'Overview' | 'Steps' | 'Evidence' | 'Actions' | 'History';

// The slide-over panel (docs/design/mockups/s2-drawer.png): clicking a queue row opens the alert in a panel over
// the queue, so the analyst can look at it and act without losing her place. Esc, ✕ or a click on the dimmed queue
// closes it; "Open full page" goes to /alerts/{id}. Same data and buttons as the full page (useAlertData), shorter.
export default function AlertDrawer({ alertId, onClose, onChanged }: {
  alertId: number;
  onClose: () => void;
  onChanged: () => void; // the queue reloads too (status, assignee, signals may have changed)
}) {
  const { alert, findings, steps, actions, escalations, history, me, error, reload, rules } = useAlertData(alertId);
  const [tab, setTab] = useState<Tab>('Overview');
  const closeRef = useRef<HTMLButtonElement>(null);

  // After any change in the panel: reload the panel and the queue behind it.
  function changed() {
    reload();
    onChanged();
  }

  // Esc closes the panel, unless a modal (e.g. "Close alert") is open: then Esc is the modal's.
  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape' && !document.querySelector('dialog[open]')) {
        onClose();
      }
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey); // clean up when the panel closes
  }, [onClose]);

  // Keyboard focus starts in the panel (on ✕), so Tab moves through the panel, not the queue behind it.
  useEffect(() => {
    closeRef.current?.focus();
  }, [alertId]);

  const done = steps.filter((s) => s.status !== 'PENDING').length;
  const tabs: { name: Tab; count?: string }[] = [
    { name: 'Overview' },
    { name: 'Steps', count: `${done}/${steps.length}` },
    { name: 'Evidence', count: String(findings.length) },
    { name: 'Actions', count: String(actions.length) },
    { name: 'History', count: String(history.length + 1) }, // + "Alert received"
  ];

  return (
    <>
      <div className="scrim" onClick={onClose} aria-hidden="true" />
      <aside className="drawer" role="dialog" aria-modal="true" aria-label={alert?.email.subject ?? 'Alert'}>
        <div className="drawer-top">
          <span className="mono muted">{alert?.ticketNumber}</span>
          <Link to={`/alerts/${alertId}`} className="button secondary">Open full page ↗</Link>
          <button type="button" className="secondary" ref={closeRef} onClick={onClose} aria-label="Close the panel">✕</button>
        </div>
        <ErrorMessage error={error} />

        {alert && (
          <>
            <div>
              <h1 className="drawer-title">{alert.email.subject}</h1>
              <div className="meta">
                <StatusChip status={alert.status} />
                <PriorityTag priority={alert.priority} />
                <span className="sep" />
                <span>{statusLabel(alert.caseType)}</span>
                <span className="sep" />
                <span className="mono">
                  {new Date(alert.receivedAt).toLocaleString([], {
                    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
                  })}
                </span>
              </div>
            </div>

            <AlertCards alert={alert} me={me} canChange={rules.canChange} onChanged={changed} />

            <nav className="tabs drawer-tabs" role="tablist">
              {tabs.map((t) => (
                <button key={t.name} type="button" role="tab" aria-selected={tab === t.name}
                        className={tab === t.name ? 'tab active' : 'tab'} onClick={() => setTab(t.name)}>
                  {t.name} {t.count !== undefined && <em>{t.count}</em>}
                </button>
              ))}
            </nav>

            <div className="drawer-body">
              {tab === 'Overview' && (rules.canWork
                ? <AiAssessmentPanel key={alert.id} alertId={alert.id} saved={alert.lastAssessment} onChanged={changed} />
                : alert.lastAssessment ? <AssessmentCard assessment={alert.lastAssessment} />
                  : <p className="muted">No AI assessment was made.</p>)}
              {tab === 'Steps' && <StepList alertId={alert.id} steps={steps} editable={rules.canWork} onChanged={changed} />}
              {tab === 'Evidence' && (
                <>
                  {rules.canWork && <p><AddFindingButton alertId={alert.id} onChanged={changed} /></p>}
                  <FindingList findings={findings} />
                  <EmailDetailsSection email={alert.email} />
                  <AlertChildrenSection alert={alert} />
                </>
              )}
              {tab === 'Actions' && (
                <>
                  {rules.canWork && <p><AddActionButton alertId={alert.id} onChanged={changed} /></p>}
                  <ActionList actions={actions} />
                  <EscalationList escalations={escalations} editable={rules.canWork} onChanged={changed} />
                </>
              )}
              {tab === 'History' && (
                <section className="sec">
                  <HistoryTimeline entries={history} steps={steps} me={me}
                                   received={{ at: alert.receivedAt, source: alert.source }} />
                </section>
              )}
            </div>

            {/* The same buttons and rules as the full page (alertRules), without Draft email. */}
            <div className="drawer-actions">
              <AlertActionButtons alert={alert} rules={rules} withDraftEmail={false} onChanged={changed}
                                  onShowActions={() => setTab('Actions')} />
            </div>
          </>
        )}
      </aside>
    </>
  );
}
