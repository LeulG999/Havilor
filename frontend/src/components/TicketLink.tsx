import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { AlertDetail } from '../api/types';

// A link to an alert, labelled with its ticket number. A note only stores alert ids, so the ticket is looked up
// (GET /api/v1/alerts/{id}); until then, or if that fails, it says "Alert #id".
// `plain`: just the label, no link (inside a form, where following a link would leave the unsaved note).
export default function TicketLink({ alertId, plain = false }: { alertId: number; plain?: boolean }) {
  const [ticket, setTicket] = useState<string | null>(null);

  useEffect(() => {
    api<AlertDetail>(`/alerts/${alertId}`).then((alert) => setTicket(alert.ticketNumber)).catch(() => setTicket(null));
  }, [alertId]);

  const label = ticket ?? `Alert #${alertId}`;
  return plain ? <span>{label}</span> : <Link to={`/alerts/${alertId}`} className="mention">{label}</Link>;
}
