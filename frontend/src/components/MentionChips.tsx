import TicketLink from './TicketLink';

// The alerts a note mentions, as small chips with ✕ to remove one. After an AI draft these are exactly the
// alerts the AI returned (only ones from her own activity: the backend checks that).
export default function MentionChips({ alertIds, onRemove }: {
  alertIds: number[];
  onRemove: (alertId: number) => void;
}) {
  if (alertIds.length === 0) {
    return <p className="muted">No alerts mentioned.</p>;
  }
  return (
    <ul className="chips">
      {alertIds.map((alertId) => (
        <li key={alertId} className="chip">
          <TicketLink alertId={alertId} plain />
          <button type="button" className="chip-remove" aria-label={`Remove alert ${alertId}`}
                  onClick={() => onRemove(alertId)}>✕</button>
        </li>
      ))}
    </ul>
  );
}
