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
