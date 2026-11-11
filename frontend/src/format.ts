// Small display helpers shared by the pages.

// "2026-10-28T14:00:00Z" → the date and time in the user's own time zone and format.
export function formatDateTime(isoText: string | null): string {
  return isoText ? new Date(isoText).toLocaleString() : '—';
}
