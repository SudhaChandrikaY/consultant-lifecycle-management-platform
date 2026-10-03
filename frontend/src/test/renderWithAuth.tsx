import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { AuthContext, type AuthContextValue } from '../auth/AuthProvider';
import type { CurrentUser, Role } from '../api/types';

export function userWithRole(role: Role, overrides: Partial<CurrentUser> = {}): CurrentUser {
  return {
    id: 1,
    username: role.toLowerCase(),
    displayName: `Test ${role}`,
    role,
    recruiterId: role === 'RECRUITER' ? 7 : null,
    sessionTimeoutMinutes: 30,
    ...overrides,
  };
}

/** Renders inside a router with a fixed signed-in user (or none). */
export function renderWithAuth(
  ui: ReactElement,
  { user, route = '/' }: { user: CurrentUser | null; route?: string },
) {
  const value: AuthContextValue = {
    user,
    loading: false,
    login: async () => {
      throw new Error('not used in tests');
    },
    logout: async () => undefined,
  };
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </AuthContext.Provider>,
  );
}
