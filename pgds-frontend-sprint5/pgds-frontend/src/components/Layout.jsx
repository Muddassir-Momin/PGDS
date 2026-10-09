import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { ROLE_LABEL, useAuth } from '../auth';
import { ROUTES } from '../routes';

export default function Layout() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const items = ROUTES.filter((r) => r.roles.includes(user.role));

  return (
    <div className="app-shell">
      <aside className={`gg-sidebar ${open ? 'open' : ''}`}>
        <div className="gg-brand">
          <i className="bi bi-flower1" />
          <div>
            <div className="gg-brand-name">GrainGuard AI</div>
            <div className="gg-brand-sub">Smart Food Grain Distribution</div>
          </div>
        </div>
        <nav className="gg-nav">
          {items.map((r) => (
            <NavLink key={r.path} to={r.path} onClick={() => setOpen(false)} className={({ isActive }) => `gg-nav-link ${isActive ? 'active' : ''}`}>
              <i className={`bi ${r.icon}`} /> {r.label}
            </NavLink>
          ))}
        </nav>
        <div className="gg-tagline">
          <div>अन्न सुरक्षा</div>
          <div>सबका अधिकार</div>
          <small>Food Security for a Stronger India</small>
        </div>
      </aside>
      {open && <div className="gg-backdrop" onClick={() => setOpen(false)} />}

      <div className="gg-main">
        <header className="gg-topbar">
          <button className="btn btn-light d-lg-none" onClick={() => setOpen(true)} aria-label="Menu"><i className="bi bi-list fs-5" /></button>
          <div className="flex-grow-1" />
          <div className="dropdown">
            <button className="btn btn-light d-flex align-items-center gap-2" data-bs-toggle="dropdown">
              <span className="gg-avatar">{user.fullName?.[0] || 'U'}</span>
              <span className="text-start d-none d-sm-block">
                <span className="d-block fw-semibold lh-1">{user.fullName}</span>
                <small className="text-muted">{ROLE_LABEL[user.role]}</small>
              </span>
            </button>
            <ul className="dropdown-menu dropdown-menu-end">
              <li><button className="dropdown-item" onClick={() => { logout(); nav('/login'); }}><i className="bi bi-box-arrow-right me-2" />Sign out</button></li>
            </ul>
          </div>
        </header>
        <main className="gg-content"><Outlet /></main>
      </div>
    </div>
  );
}
