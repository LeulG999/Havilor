// A round avatar with someone's initials: "Dana Analyst" → "DA". Used in the rail and next to assignees.
export function initials(name: string): string {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
}

export default function Avatar({ name, small = false }: { name: string; small?: boolean }) {
  return <span className={small ? 'avatar small' : 'avatar'} aria-hidden="true">{initials(name)}</span>;
}
