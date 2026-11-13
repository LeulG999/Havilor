import type { AlertStep, AuditEntry, User } from './api/types';
import { statusLabel } from './format';

// Turns audit entries into readable timeline events: an icon, a title and a detail line.
// The backend's audit texts are short technical snapshots ("step 2: DONE, answer=Checked"); this file reads them
// (they are written by the backend's services, so their shape is known) and says the same thing in plain words.

export type EventIcon = 'bell' | 'check' | 'user' | 'spark' | 'flag' | 'doc' | 'bolt' | 'lock' | 'dot';

export type TimelineEvent = {
  key: string;
  icon: EventIcon;
  title: string;
  detail: string | null;
  at: string; // ISO time
  who: string | null; // "You", "User #3", or null (the system)
};

// "status=NEW, priority=MEDIUM, assigneeId=null" → { status: 'NEW', priority: 'MEDIUM', assigneeId: 'null' }.
function fields(text: string | null): Record<string, string> {
  const result: Record<string, string> = {};
  for (const match of (text ?? '').matchAll(/(\w+)=([^,]*)/g)) {
    result[match[1]] = match[2];
  }
  return result;
}

// Everything after "key=" to the end ("reason" may itself contain commas).
function rest(text: string | null, key: string): string | null {
  const match = new RegExp(`${key}=(.*)$`).exec(text ?? '');
  return match ? match[1] : null;
}

function nice(value: string): string {
  return value === 'null' ? '—' : statusLabel(value);
}

export function describe(entry: AuditEntry, steps: AlertStep[], me: User | null): TimelineEvent {
  const who = entry.actorId === null ? null : entry.actorId === me?.id ? 'You' : `User #${entry.actorId}`;
  const base = { key: `audit-${entry.id}`, at: entry.createdAt, who };
  const after = entry.newValue;

  switch (entry.action) {
    case 'ALERT_UPDATED': {
      const before = fields(entry.oldValue);
      const now = fields(after);
      const changed = Object.keys(now).filter((k) => now[k] !== before[k]);
      if (changed.includes('assigneeId')) {
        const assignee = now.assigneeId === String(me?.id) ? 'you' : `user #${now.assigneeId}`;
        const status = changed.includes('status') ? `Status ${before.status} → ${now.status}` : null;
        return { ...base, icon: 'user', title: `Assigned to ${assignee}`, detail: status };
      }
      return {
        ...base, icon: 'dot', title: 'Alert updated',
        detail: changed.map((k) => `${k} ${nice(before[k] ?? 'null')} → ${nice(now[k])}`).join(' · ') || null,
      };
    }
    case 'STEP_UPDATED': {
      const match = /step (\d+): (\w+)/.exec(after ?? '');
      const step = match ? steps.find((s) => s.stepId === Number(match[1])) : undefined;
      const status = match?.[2] ?? '';
      const verb = status === 'DONE' ? 'Step done' : status === 'SKIPPED' ? 'Step skipped' : 'Step reopened';
      const answer = rest(after, 'answer');
      return {
        ...base, icon: 'check', title: step ? `${verb}: ${step.title}` : verb,
        detail: answer ? `Answer: ${answer}` : null,
      };
    }
    case 'FINDING_ADDED':
      return { ...base, icon: 'doc', title: 'Finding added', detail: rest(after, 'title') };
    case 'ACTION_RECORDED': {
      const match = /action \d+: (\w+) (\w+)/.exec(after ?? '');
      return {
        ...base, icon: 'bolt', title: match ? `Action: ${statusLabel(match[1])}` : 'Action recorded',
        detail: match ? `Result: ${statusLabel(match[2]).toLowerCase()}` : null,
      };
    }
    case 'ESCALATED': {
      const team = /team=([^,]*)/.exec(after ?? '')?.[1];
      return { ...base, icon: 'flag', title: team ? `Escalated to ${team}` : 'Escalated', detail: rest(after, 'reason') };
    }
    case 'ESCALATION_UPDATED': {
      const now = fields(after);
      const outcome = now.outcome && now.outcome !== 'null' ? `outcome ${nice(now.outcome)}` : null;
      return {
        ...base, icon: 'flag', title: 'Escalation updated',
        detail: [now.externalStatus ? `their status ${nice(now.externalStatus)}` : null, outcome].filter(Boolean).join(' · ') || null,
      };
    }
    case 'ALERT_CLOSED':
      return { ...base, icon: 'lock', title: 'Alert closed', detail: `Verdict: ${nice(fields(after).verdict ?? 'null')}` };
    case 'AI_SUGGESTION':
      return {
        ...base, icon: 'spark', title: 'AI assessment',
        detail: `Suggested ${fields(after).suggestedVerdict ?? '—'}`,
      };
    default:
      return { ...base, icon: 'dot', title: statusLabel(entry.action), detail: after };
  }
}

// The alert's whole story: "Alert received" (from the alert itself), then every audit entry, oldest first.
export function timeline(entries: AuditEntry[], steps: AlertStep[], me: User | null,
                         received?: { at: string; source: string }): TimelineEvent[] {
  const events = entries.map((entry) => describe(entry, steps, me));
  if (received) {
    events.unshift({
      key: 'received', icon: 'bell', title: 'Alert received',
      detail: `From ${statusLabel(received.source).toLowerCase()}`, at: received.at, who: null,
    });
  }
  return events;
}
