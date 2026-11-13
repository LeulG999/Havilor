import { Link, useLocation } from 'react-router';

// The bar at the top of every logged-in page: a breadcrumb that says where you are ("Alerts / Queue").
// It is worked out from the URL only, so it needs no data. (The mockups' search box and bell are left out until
// they do something.)
type Crumb = { section: string; sectionLink?: string; page: string };

function crumbFor(path: string): Crumb {
  if (path === '/dashboard') return { section: 'Home', page: 'Dashboard' };
  if (path === '/alerts') return { section: 'Alerts', sectionLink: '/alerts', page: 'Queue' };
  if (path === '/alerts/new') return { section: 'Alerts', sectionLink: '/alerts', page: 'New alert' };
  if (path.startsWith('/alerts/')) return { section: 'Alerts', sectionLink: '/alerts', page: 'Alert' };
  if (path === '/standup') return { section: 'Standup', page: 'My notes' };
  if (path === '/team-standups') return { section: 'Manager', page: 'Team standups' };
  if (path === '/audit') return { section: 'Manager', page: 'Audit log' };
  if (path === '/team') return { section: 'Manager', page: 'Team' };
  return { section: 'Havilor', page: '' };
}

export default function TopBar() {
  const crumb = crumbFor(useLocation().pathname);
  return (
    <header className="topbar">
      <nav className="crumb" aria-label="Breadcrumb">
        {crumb.sectionLink ? <Link to={crumb.sectionLink}>{crumb.section}</Link> : <span>{crumb.section}</span>}
        {crumb.page && <> / <b>{crumb.page}</b></>}
      </nav>
    </header>
  );
}
