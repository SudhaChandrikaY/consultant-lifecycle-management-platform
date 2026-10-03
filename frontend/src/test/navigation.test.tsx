import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useEffect } from 'react';
import { Route, Routes } from 'react-router';
import NavBar from '../components/NavBar';
import RequireRole from '../auth/RequireRole';
import type { Role } from '../api/types';
import { renderWithAuth, userWithRole } from './renderWithAuth';

// contracts/authorization-matrix.md § Frontend navigation
const EXPECTED_NAV: Record<Role, string[]> = {
  ADMIN: ['Dashboard', 'Recruiters', 'Consultants', 'Marketing', 'Submissions', 'Placements', 'Reports'],
  MANAGER: ['Dashboard', 'Recruiters', 'Consultants', 'Marketing', 'Submissions', 'Placements', 'Reports'],
  RECRUITER: ['Dashboard', 'Consultants', 'Marketing', 'Submissions', 'Placements'],
  HR_OPERATIONS: ['Dashboard', 'Consultants'],
};

describe('NavBar', () => {
  it.each(Object.keys(EXPECTED_NAV) as Role[])('shows exactly the permitted items for %s', (role) => {
    renderWithAuth(<NavBar />, { user: userWithRole(role) });
    const nav = screen.getByRole('navigation', { name: /main/i });
    const labels = within(nav)
      .getAllByRole('link')
      .map((a) => a.textContent);
    expect(labels).toEqual(EXPECTED_NAV[role]);
  });

  it('shows the display name and role label with a sign-out button', () => {
    renderWithAuth(<NavBar />, { user: userWithRole('HR_OPERATIONS', { displayName: 'Harper HR' }) });
    expect(screen.getByText('Harper HR')).toBeInTheDocument();
    expect(screen.getByText('HR Operations')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument();
  });
});

describe('RequireRole', () => {
  function AreaPage({ onLoad }: { onLoad: () => void }) {
    useEffect(() => {
      onLoad();
    }, [onLoad]);
    return <p>Area data</p>;
  }

  it.each([
    ['/recruiters', ['ADMIN', 'MANAGER']],
    ['/reports', ['ADMIN', 'MANAGER']],
  ] as const)('refuses RECRUITER on %s without calling the area API', (route, roles) => {
    const loader = vi.fn();
    renderWithAuth(
      <RequireRole roles={[...roles]}>
        <AreaPage onLoad={loader} />
      </RequireRole>,
      { user: userWithRole('RECRUITER'), route },
    );
    expect(screen.getByRole('heading', { name: /not authorized/i })).toBeInTheDocument();
    expect(screen.queryByText('Area data')).not.toBeInTheDocument();
    expect(loader).not.toHaveBeenCalled();
  });

  it('renders the page for a permitted role', () => {
    const loader = vi.fn();
    renderWithAuth(
      <RequireRole roles={['ADMIN', 'MANAGER']}>
        <AreaPage onLoad={loader} />
      </RequireRole>,
      { user: userWithRole('MANAGER'), route: '/recruiters' },
    );
    expect(screen.getByText('Area data')).toBeInTheDocument();
    expect(loader).toHaveBeenCalled();
  });

  it('redirects to /login when nobody is signed in', () => {
    renderWithAuth(
      <Routes>
        <Route path="/login" element={<p>Login page</p>} />
        <Route
          path="/recruiters"
          element={
            <RequireRole roles={['ADMIN']}>
              <p>Secret</p>
            </RequireRole>
          }
        />
      </Routes>,
      { user: null, route: '/recruiters' },
    );
    expect(screen.getByText('Login page')).toBeInTheDocument();
    expect(screen.queryByText('Secret')).not.toBeInTheDocument();
  });
});
