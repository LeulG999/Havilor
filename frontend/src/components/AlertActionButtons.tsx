import type { AlertDetail } from '../api/types';
import type { AlertRules } from '../alertRules';
import CloseAlertButton from './CloseAlertButton';
import DraftEmailButton from './DraftEmailButton';
import EscalateButton from './EscalateButton';

// The alert-wide buttons (full page header and slide-over panel), driven only by alertRules:
// - nothing at all unless the analyst can work on this alert (open, role ANALYST);
// - while ESCALATED: a chip "⇡ Escalated to <team> · waiting" instead of Escalate (it opens the Actions tab, where
//   the team's answer is recorded), and Close alert disabled with the reason;
// - otherwise: Escalate, (Draft email,) Close alert.
// The backend refuses the same things with 409, and those messages show in the modals.
export default function AlertActionButtons({ alert, rules, withDraftEmail, onChanged, onShowActions }: {
  alert: AlertDetail;
  rules: AlertRules;
  withDraftEmail: boolean;
  onChanged: () => void;
  onShowActions: () => void;
}) {
  if (!rules.canWork) {
    return null;
  }
  return (
    <div className="buttons">
      {rules.isEscalated ? (
        <button type="button" className="secondary escalated-chip" onClick={onShowActions}
                title="Open the Actions tab to record the team's answer">
          ⇡ Escalated to {rules.waitingOn ?? 'another team'} · waiting
        </button>
      ) : (
        rules.canEscalate && <EscalateButton alertId={alert.id} onChanged={onChanged} />
      )}
      {withDraftEmail && <DraftEmailButton alertId={alert.id} />}
      <CloseAlertButton alertId={alert.id} onChanged={onChanged}
                        disabledReason={rules.canClose ? null : "Record the team's answer first"} />
    </div>
  );
}
