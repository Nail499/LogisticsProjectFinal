import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext.jsx';
import Logo from './Logo.jsx';
import NotificationBell from './NotificationBell.jsx';
import ThemeToggle from './ThemeToggle.jsx';
import PushToggle from './PushToggle.jsx';
import LanguageSwitcher from './LanguageSwitcher.jsx';

export default function DashboardLayout({ title, links, children, theme }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className={`app-shell${theme ? ` theme-${theme}` : ''}`}>
      <aside className="sidebar">
        <div className="sidebar-brand">
          <Logo size={26} variant={theme === 'orange' ? 'dark' : 'light'} />
        </div>
        <nav className="sidebar-nav">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) => 'sidebar-link' + (isActive ? ' active' : '')}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div style={{ color: '#9ca3af', fontSize: 13, marginBottom: 10 }}>
            {user?.username} · {user?.role}
          </div>
          <button className="btn btn-sm btn-block" onClick={handleLogout}>
            {t('common.logout')}
          </button>
        </div>
      </aside>
      <div className="main-area">
        <div className="topbar">
          <h3 style={{ margin: 0 }}>{title}</h3>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <LanguageSwitcher variant="app" />
            <ThemeToggle />
            <PushToggle />
            <NotificationBell />
          </div>
        </div>
        <div className="content" key={location.pathname}>{children}</div>
      </div>

      {/* Stage 5 — mobile bottom nav: the sidebar disappears below 600px
          (see index.css), so small-screen users (Driver PWA in particular)
          need another way to switch sections. */}
      <nav className="mobile-bottom-nav">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.end}
            className={({ isActive }) => 'mobile-bottom-nav-link' + (isActive ? ' active' : '')}
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
