// A row of tab buttons. It only shows which tab is active and reports clicks; the page decides what to show.
export default function Tabs<T extends string>({ tabs, active, onSelect }: {
  tabs: T[];
  active: T;
  onSelect: (tab: T) => void;
}) {
  return (
    <nav className="tabs" role="tablist">
      {tabs.map((tab) => (
        <button key={tab} type="button" role="tab" aria-selected={tab === active}
                className={tab === active ? 'tab active' : 'tab'} onClick={() => onSelect(tab)}>
          {tab}
        </button>
      ))}
    </nav>
  );
}
