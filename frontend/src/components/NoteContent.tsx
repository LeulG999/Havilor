import TicketLink from './TicketLink';

// The body of a standup note, exactly as the team (and the manager) sees it: the "Yesterday:", "Today:" and
// "Blockers:" sections as small headings with their "- " lines as bullets, then the mentioned alerts as ticket
// links. Used on the note cards (my notes, Team standups) AND as the live preview in the note editor, so the
// analyst sees exactly what the manager will see.

const SECTION = /^(Yesterday|Today|Blockers):\s*(.*)$/;

type Block = { title: string | null; lines: string[] };

// "Yesterday:\n- Blocked a sender.\nToday:\n- Nothing." → [{title: "Yesterday:", lines: ["Blocked a sender."]}, ...].
// Text before the first section (or a note written without sections) becomes a block without a title.
function toBlocks(text: string): Block[] {
  const blocks: Block[] = [];
  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim();
    if (line === '') {
      continue;
    }
    const section = SECTION.exec(line);
    if (section) {
      blocks.push({ title: `${section[1]}:`, lines: section[2] ? [section[2]] : [] });
      continue;
    }
    if (blocks.length === 0) {
      blocks.push({ title: null, lines: [] });
    }
    blocks[blocks.length - 1].lines.push(line);
  }
  return blocks;
}

export default function NoteContent({ text, alertIds }: { text: string; alertIds: number[] }) {
  return (
    <div className="note-content">
      {toBlocks(text).map((block, i) => block.title === null ? (
        // No section: plain lines, as typed.
        <div key={i}>{block.lines.map((line, j) => <p key={j}>{line}</p>)}</div>
      ) : (
        <div key={i}>
          <div className="note-section">{block.title}</div>
          <ul>{block.lines.map((line, j) => <li key={j}>{line.replace(/^- /, '')}</li>)}</ul>
        </div>
      ))}
      {alertIds.length > 0 && (
        <p className="muted">
          Mentions: {alertIds.map((alertId) => <TicketLink key={alertId} alertId={alertId} />)}
        </p>
      )}
    </div>
  );
}
