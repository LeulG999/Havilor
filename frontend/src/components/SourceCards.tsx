// Step 1 of "New alert": what the analyst might have, best first, so she knows what to bring. Each card says what
// Havilor can read from it and how complete that is (3 dots = everything), and for the two best sources how to get
// them. A native <details> element is the "How do I get this?" expander: no state needed, keyboard-friendly.

type Source = { name: string; best?: boolean; quality: 1 | 2 | 3; gives: string; how?: string[] };

const SOURCES: Source[] = [
  {
    name: 'Email file (.eml)', best: true, quality: 3,
    gives: 'Everything: headers, SPF/DKIM/DMARC, sender, recipients, links and attachment names.',
    how: ['Outlook on the web or Gmail: open the message → Download / Download original.'],
  },
  {
    name: 'Raw headers', quality: 2,
    gives: 'Sender, recipients, date and SPF/DKIM/DMARC, but not the links or attachments in the body.',
    how: [
      'Outlook desktop: File → Properties → Internet headers.',
      'Gmail: ⋮ → Show original.',
    ],
  },
  {
    name: 'Screenshot or PDF', quality: 1,
    gives: 'What is visible: subject, sender name, the text and the links you can see. No checks.',
  },
  {
    name: 'Pasted text', quality: 1,
    gives: 'The subject, sender and links you paste. No checks unless you paste the headers too.',
  },
];

export default function SourceCards() {
  return (
    <div className="source-cards">
      {SOURCES.map((source) => (
        <div key={source.name} className={source.best ? 'source-card best' : 'source-card'}>
          <div className="source-head">
            <b>{source.name}</b>
            {source.best && <span className="best-tag">Best</span>}
            <span className="quality" aria-label={`Quality ${source.quality} of 3`}>
              {[1, 2, 3].map((dot) => <i key={dot} className={dot <= source.quality ? 'on' : ''} aria-hidden="true" />)}
            </span>
          </div>
          <p>{source.gives}</p>
          {source.how && (
            <details>
              <summary>How do I get this?</summary>
              <ul className="how">{source.how.map((step) => <li key={step}>{step}</li>)}</ul>
            </details>
          )}
        </div>
      ))}
    </div>
  );
}
