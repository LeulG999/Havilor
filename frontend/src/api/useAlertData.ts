import { useEffect, useState } from 'react';
import { api } from './api';
import { alertRules } from '../alertRules';
import type { Action, AlertDetail, AlertStep, AuditEntry, Escalation, Finding, User } from './types';

// Everything a view of one alert needs: the alert, its findings, steps, actions, escalations and history, plus who
// is logged in. A custom hook (a function that uses useState/useEffect) so the full alert page and the slide-over
// panel on the queue load the data the same way. reload() fetches it all again after a change.
export function useAlertData(alertId: number | string | undefined) {
  const [alert, setAlert] = useState<AlertDetail | null>(null);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [steps, setSteps] = useState<AlertStep[]>([]);
  const [actions, setActions] = useState<Action[]>([]);
  const [escalations, setEscalations] = useState<Escalation[]>([]);
  const [history, setHistory] = useState<AuditEntry[]>([]);
  const [me, setMe] = useState<User | null>(null);
  const [error, setError] = useState<unknown>(null);

  // All six requests at the same time (Promise.all); if any fails, the error is shown.
  function reload() {
    setError(null);
    Promise.all([
      api<AlertDetail>(`/alerts/${alertId}`),
      api<Finding[]>(`/alerts/${alertId}/findings`),
      api<AlertStep[]>(`/alerts/${alertId}/steps`),
      api<Action[]>(`/alerts/${alertId}/actions`),
      api<Escalation[]>(`/alerts/${alertId}/escalations`),
      api<AuditEntry[]>(`/alerts/${alertId}/audit`),
    ])
      .then(([alertAnswer, findingsAnswer, stepsAnswer, actionsAnswer, escalationsAnswer, historyAnswer]) => {
        setAlert(alertAnswer);
        setFindings(findingsAnswer);
        setSteps(stepsAnswer);
        setActions(actionsAnswer);
        setEscalations(escalationsAnswer);
        setHistory(historyAnswer);
      })
      .catch(setError);
  }

  useEffect(reload, [alertId]);

  // Who is logged in: for "Assign to me", "(you)" and which buttons a role gets.
  useEffect(() => {
    api<User>('/users/me').then(setMe).catch(setError);
  }, []);

  // What may be done right now: the one shared rule (alertRules.ts).
  const rules = alertRules(alert, me, escalations);

  return { alert, findings, steps, actions, escalations, history, me, error, reload, rules };
}
