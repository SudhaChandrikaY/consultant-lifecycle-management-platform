import { NavLink } from 'react-router';
import { useAuth } from '../auth/AuthProvider';
import { navItemsFor, ROLE_LABELS } from '../auth/permissions';

export default function NavBar() {
  const { user, logout } = useAuth();
  if (!user) return null;
  return (
    <header className="nav">
      <span className="nav__brand">CLMP</span>
      <nav aria-label="Main" style={{ flex: 1, minWidth: 0 }}>
        <ul className="nav__items">
          {navItemsFor(user.role).map((item) => (
            <li key={item.path}>
              <NavLink to={item.path} end={item.path === '/'}>
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="nav__user">
        <span>{user.displayName}</span>
        <span className="nav__role">{ROLE_LABELS[user.role]}</span>
        <button type="button" onClick={() => void logout()}>
          Sign out
        </button>
      </div>
    </header>
  );
}
