import type { AlertDetail, Escalation, User } from './api/types';

// What may be done with an alert right now: ONE place for the rule, used by the full page, the slide-over panel,
// their tabs, forms and in-place cards, so they can't drift apart. The backend enforces the same rules (it answers
// 403 / 409 otherwise); this only decides what to show.
export type AlertRules = {
  isOpen: boolean;          // not CLOSED: something can still change
  isEscalated: boolean;     // waiting on another team's answer
  canChange: boolean;       // the in-place cards (priority, assignee): any role, open alerts only
  canWork: boolean;         // the investigation (steps, findings, actions, AI, escalation answers): analysts, open alerts
  canEscalate: boolean;     // a new escalation: only when not already waiting on one (Flow F31)
  canClose: boolean;        // closing: not while waiting on an escalation (Flow F31)
  waitingOn: string | null; // the team of the escalation without an answer yet, if any
};

export function alertRules(alert: AlertDetail | null, me: User | null, escalations: Escalation[]): AlertRules {
  const isOpen = alert !== null && alert.status !== 'CLOSED';
  const isEscalated = alert?.status === 'ESCALATED';
  const canWork = isOpen && me?.role === 'ANALYST';
  const open = escalations.filter((e) => e.outcome === null);
  return {
    isOpen,
    isEscalated,
    canChange: isOpen,
    canWork,
    canEscalate: canWork && !isEscalated,
    canClose: canWork && !isEscalated,
    waitingOn: isEscalated && open.length > 0 ? open[open.length - 1].team : null,
  };
}
