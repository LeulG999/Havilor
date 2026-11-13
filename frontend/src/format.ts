// Small display helpers shared by the pages.

// "2026-10-28T14:00:00Z" → the date and time in the user's own time zone and format.
export function formatDateTime(isoText: string | null): string {
  return isoText ? new Date(isoText).toLocaleString() : '—';
}

// 52 → "2 d 4 h", 5 → "5 h": how long something has been waiting.
export function formatAge(hours: number): string {
  const days = Math.floor(hours / 24);
  return days > 0 ? `${days} d ${hours % 24} h` : `${hours} h`;
}

// The queue's "Received" column: the time for today ("10:07"), otherwise the day ("Nov 10").
export function formatQueueTime(isoText: string): string {
  const date = new Date(isoText);
  const today = new Date();
  return date.toDateString() === today.toDateString()
    ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
    : date.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

// "Dana Analyst" → "Dana A.": short enough for a table column.
export function shortName(name: string): string {
  const [first, ...rest] = name.split(/\s+/);
  return rest.length === 0 ? first : `${first} ${rest[rest.length - 1][0]}.`;
}

// NEW → "New", WAITING_ON_EXTERNAL → "Waiting on external".
export function statusLabel(status: string): string {
  const words = status.toLowerCase().split('_').join(' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}
