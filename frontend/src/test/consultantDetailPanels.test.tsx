import { screen, within } from '@testing-library/react';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { client } from '../api/client';
import type { ConsultantDetail, Role } from '../api/types';
import ConsultantDetailPage from '../pages/consultants/ConsultantDetailPage';
import { renderWithAuth, userWithRole } from './renderWithAuth';

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>();
  return { ...actual, client: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() } };
});

const mocked = vi.mocked(client);

// A detail payload carrying every commercial panel, so the HR case proves the page itself hides them.
export const FULL_DETAIL = {
  id: 1,
  firstName: 'Arun',
  lastName: 'Kumar',
  primarySkill: 'Java',
  yearsExperience: 8,
  visaType: 'H1B',
  status: 'MARKETING',
  needsReassignment: false,
  assignedRecruiter: { id: 7, fullName: 'Riya Patel', status: 'ACTIVE' },
  contact: { email: 'arun@example.com', phone: '555', visaExpirationDate: null, notes: null },
  allowedStatusTransitions: [],
  missingReadinessItems: [],
  version: 3,
  currentMarketingAssignment: { id: 44, status: 'ACTIVE', targetDate: '2026-10-01', overdue: true },
} as ConsultantDetail;

export function renderDetail(role: Role, detail: ConsultantDetail = FULL_DETAIL) {
  mocked.get.mockImplementation(async (path: string) => {
    if (path === '/api/consultants/1') return detail;
    if (path === '/api/consultants/1/history') return { items: [], page: 0, size: 50, totalItems: 0, totalPages: 0 };
    throw new Error(`unexpected GET ${path}`);
  });
  return renderWithAuth(
    <Routes>
      <Route path="/consultants/:id" element={<ConsultantDetailPage />} />
    </Routes>,
    { user: userWithRole(role), route: '/consultants/1' },
  );
}

describe('Consultant detail — Marketing panel', () => {
  beforeEach(() => vi.resetAllMocks());

  it.each(['ADMIN', 'MANAGER', 'RECRUITER'] as Role[])('renders for %s', async (role) => {
    renderDetail(role);
    const panel = await screen.findByRole('region', { name: /marketing/i });
    expect(within(panel).getByText('Active')).toBeInTheDocument();
    expect(within(panel).getByText(/overdue/i)).toBeInTheDocument();
    expect(within(panel).getByRole('link', { name: /view assignment/i })).toHaveAttribute('href', '/marketing/44');
  });

  it('shows the empty message and a Start marketing action for a Ready consultant without one', async () => {
    renderDetail('RECRUITER', { ...FULL_DETAIL, status: 'READY', currentMarketingAssignment: undefined });
    const panel = await screen.findByRole('region', { name: /marketing/i });
    expect(within(panel).getByText('No open marketing assignment')).toBeInTheDocument();
    expect(within(panel).getByRole('link', { name: /start marketing/i })).toHaveAttribute(
      'href',
      '/marketing/new?consultantId=1',
    );
  });

  it('is not rendered for HR_OPERATIONS even if the data were present', async () => {
    renderDetail('HR_OPERATIONS');
    expect(await screen.findByRole('heading', { name: /arun kumar/i })).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: /marketing/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/overdue/i)).not.toBeInTheDocument();
  });
});
