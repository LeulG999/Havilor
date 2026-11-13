// The few icons the app uses, as small inline SVGs (no icon library). They draw with `currentColor`, so CSS decides
// their colour (e.g. cyan for the active rail link).

import type { ReactNode } from 'react';

type IconProps = { size?: number };

function Svg({ size = 18, children }: IconProps & { children: ReactNode }) {
  return (
    <svg width={size} height={size} viewBox="0 0 18 18" aria-hidden="true" focusable="false">{children}</svg>
  );
}

export function DashboardIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <rect x="2" y="2" width="6" height="6" rx="1.5" fill="currentColor" />
      <rect x="10" y="2" width="6" height="6" rx="1.5" fill="currentColor" opacity=".55" />
      <rect x="2" y="10" width="6" height="6" rx="1.5" fill="currentColor" opacity=".55" />
      <rect x="10" y="10" width="6" height="6" rx="1.5" fill="currentColor" />
    </Svg>
  );
}

export function QueueIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <path d="M3 4.5h12M3 9h12M3 13.5h8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </Svg>
  );
}

export function PlusIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <path d="M9 3v12M3 9h12" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </Svg>
  );
}

export function NoteIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <rect x="3" y="2.5" width="12" height="13" rx="2" fill="none" stroke="currentColor" strokeWidth="1.6" />
      <path d="M6 7h6M6 10.5h4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </Svg>
  );
}

export function TeamIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <circle cx="6.5" cy="6.5" r="2.4" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="12.5" cy="7.5" r="1.9" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <path d="M2.5 15c.5-2.6 2-3.9 4-3.9s3.5 1.3 4 3.9M11 11.3c1.9-.2 3.6.9 4.2 3.2" fill="none" stroke="currentColor"
            strokeWidth="1.5" strokeLinecap="round" />
    </Svg>
  );
}

export function AuditIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <circle cx="9" cy="9" r="6.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
      <path d="M9 5.3V9l2.6 1.7" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </Svg>
  );
}

export function PeopleIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <circle cx="7" cy="6" r="2.6" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <path d="M2.5 15c.5-2.8 2.2-4.2 4.5-4.2 1.3 0 2.4.4 3.2 1.3M13.5 10v5M11 12.5h5" fill="none" stroke="currentColor"
            strokeWidth="1.5" strokeLinecap="round" />
    </Svg>
  );
}

export function KeyIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <circle cx="6" cy="9" r="3.2" fill="none" stroke="currentColor" strokeWidth="1.6" />
      <path d="M9.2 9H16M13.5 9v2.5M16 9v2" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </Svg>
  );
}

export function LogoutIcon(props: IconProps) {
  return (
    <Svg {...props}>
      <path d="M7 3H4.5A1.5 1.5 0 0 0 3 4.5v9A1.5 1.5 0 0 0 4.5 15H7M11.5 12.5 15 9l-3.5-3.5M15 9H7" fill="none"
            stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </Svg>
  );
}
