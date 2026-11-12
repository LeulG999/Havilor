import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { AlertDetail } from '../api/types';

// A link to an alert, labelled with its ticket number. A note only stores alert ids, so the ticket is looked up
// (GET /api/v1/alerts/{id}); until then, or if that fails, the link says "Alert #id".
export default function TicketLink({ alertId }: { alertId: number }) {
  const [ticket, setTicket] = useState<string | null>(null);

  useEffect(() => {
    api<AlertDetail>(`/alerts/${alertId}`).then((alert) => setTicket(alert.ticketNumber)).catch(() => setTicket(null));
  }, [alertId]);

  return <Link to={`/alerts/${alertId}`} className="mention">{ticket ?? `Alert #${alertId}`}</Link>;
}
