import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, client } from '../api/client';
import SubmissionFormPage from '../pages/submissions/SubmissionFormPage';
import { renderWithAuth, userWithRole } from './renderWithAuth';

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>();
  return { ...actual, client: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() } };
});

const mocked = vi.mocked(client);

const DUPLICATE = new ApiError({
  status: 409,
  code: 'DUPLICATE_SUBMISSION',
  detail: 'Matching earlier submissions exist.',
  duplicates: [
    { id: 31, status: 'WITHDRAWN', submittedDate: '2026-09-01', createdAt: '2026-09-01T12:00:00Z', recruiterName: 'Riya Patel' },
    { id: 32, status: 'REJECTED', submittedDate: '2026-09-10', createdAt: '2026-09-10T12:00:00Z', recruiterName: 'Riya Patel' },
  ],
});

async function fillAndSubmit() {
  renderWithAuth(
    <Routes>
      <Route path="/submissions/new" element={<SubmissionFormPage />} />
      <Route path="/submissions/:id" element={<p>Submission page</p>} />
    </Routes>,
    { user: userWithRole('RECRUITER'), route: '/submissions/new?consultantId=12' },
  );
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/^vendor/i), 'Acme Staffing');
  await user.type(screen.getByLabelText(/^client/i), 'Globex');
  await user.type(screen.getByLabelText(/job title/i), 'Java Developer');
  await user.type(screen.getByLabelText(/bill rate/i), '85');
  await user.click(screen.getByRole('button', { name: /save submission/i }));
  return user;
}

describe('Submission duplicate warning', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    mocked.get.mockImplementation(async (path: string) => {
      if (path === '/api/consultants/12') {
        return { id: 12, firstName: 'Arun', lastName: 'Kumar', status: 'MARKETING', allowedStatusTransitions: [], missingReadinessItems: [], needsReassignment: false, version: 1 };
      }
      if (path === '/api/vendors' || path === '/api/clients') return [];
      throw new Error(`unexpected GET ${path}`);
    });
  });

  it('lists the duplicates; Cancel sends nothing more', async () => {
    mocked.post.mockRejectedValueOnce(DUPLICATE);
    const user = await fillAndSubmit();
    const dialog = await screen.findByRole('dialog', { name: /possible duplicate/i });
    expect(within(dialog).getByText(/withdrawn/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/rejected/i)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: /cancel/i }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(mocked.post).toHaveBeenCalledTimes(1);
  });

  it('Confirm re-POSTs the same body with acknowledgeDuplicate: true', async () => {
    mocked.post.mockRejectedValueOnce(DUPLICATE).mockResolvedValueOnce({ id: 99 });
    const user = await fillAndSubmit();
    const dialog = await screen.findByRole('dialog', { name: /possible duplicate/i });
    await user.click(within(dialog).getByRole('button', { name: /submit anyway/i }));
    await waitFor(() => expect(mocked.post).toHaveBeenCalledTimes(2));
    const [, firstBody] = mocked.post.mock.calls[0];
    const [path, secondBody] = mocked.post.mock.calls[1];
    expect(path).toBe('/api/submissions');
    expect(firstBody).toMatchObject({ acknowledgeDuplicate: false, vendorName: 'Acme Staffing', clientName: 'Globex' });
    expect(secondBody).toEqual({ ...(firstBody as object), acknowledgeDuplicate: true });
    expect(await screen.findByText('Submission page')).toBeInTheDocument();
  });
});
