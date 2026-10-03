import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import DataTable from '../components/DataTable';
import type { Role } from '../api/types';
import { renderWithAuth, userWithRole } from './renderWithAuth';

const EMPTY_PAGE = { items: [] as { id: number; name: string }[], page: 0, size: 25, totalItems: 0, totalPages: 0 };
const UNLINKED = 'Your account is not linked to a recruiter profile. Contact an Admin.';

function renderTable(role: Role, recruiterId: number | null) {
  renderWithAuth(
    <DataTable
      columns={[{ key: 'name', header: 'Name', render: (r: { id: number; name: string }) => r.name }]}
      page={EMPTY_PAGE}
      rowKey={(r) => r.id}
      emptyMessage="No consultants match these filters."
    />,
    { user: userWithRole(role, { recruiterId }) },
  );
}

describe('shared list empty state', () => {
  it('tells an unlinked recruiter to contact an Admin', () => {
    renderTable('RECRUITER', null);
    expect(screen.getByText(UNLINKED)).toBeInTheDocument();
    expect(screen.queryByText('No consultants match these filters.')).not.toBeInTheDocument();
  });

  it.each([
    ['RECRUITER', 7],
    ['ADMIN', null],
    ['HR_OPERATIONS', null],
  ] as const)('shows the generic message for %s', (role, recruiterId) => {
    renderTable(role, recruiterId);
    expect(screen.getByText('No consultants match these filters.')).toBeInTheDocument();
    expect(screen.queryByText(UNLINKED)).not.toBeInTheDocument();
  });
});
