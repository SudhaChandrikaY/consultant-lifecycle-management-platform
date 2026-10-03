import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, client } from '../api/client';
import ConsultantFormPage from '../pages/consultants/ConsultantFormPage';
import { renderWithAuth, userWithRole } from './renderWithAuth';
import { REFERENCE } from './fixtures';

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>();
  return { ...actual, client: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() } };
});

const mocked = vi.mocked(client);

describe('ConsultantFormPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    mocked.get.mockImplementation(async (path: string) => {
      if (path === '/api/reference') return REFERENCE;
      throw new Error(`unexpected GET ${path}`);
    });
  });

  it('renders each server fieldErrors message next to its field', async () => {
    mocked.post.mockRejectedValue(
      new ApiError({
        status: 400,
        code: 'VALIDATION_FAILED',
        detail: 'One or more fields are invalid.',
        fieldErrors: [
          { field: 'firstName', message: 'must not be blank' },
          { field: 'email', message: 'must be a well-formed email address' },
          { field: 'yearsExperience', message: 'must be less than or equal to 50' },
        ],
      }),
    );

    renderWithAuth(
      <Routes>
        <Route path="/consultants/new" element={<ConsultantFormPage />} />
      </Routes>,
      { user: userWithRole('HR_OPERATIONS'), route: '/consultants/new' },
    );

    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/last name/i), 'Kumar');
    await user.type(screen.getByLabelText(/^email/i), 'not-an-email');
    await user.type(screen.getByLabelText(/years of experience/i), '51');
    await user.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() =>
      expect(screen.getByLabelText(/first name/i)).toHaveAccessibleDescription('must not be blank'),
    );
    expect(screen.getByLabelText(/^email/i)).toHaveAccessibleDescription('must be a well-formed email address');
    expect(screen.getByLabelText(/years of experience/i)).toHaveAccessibleDescription(
      'must be less than or equal to 50',
    );
    expect(screen.getByLabelText(/first name/i)).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText(/last name/i)).toHaveAttribute('aria-invalid', 'false');
    expect(mocked.post).toHaveBeenCalledWith(
      '/api/consultants',
      expect.objectContaining({ lastName: 'Kumar', yearsExperience: 51 }),
    );
  });

  it('shows the duplicate email conflict inline on the email field', async () => {
    mocked.post.mockRejectedValue(
      new ApiError({
        status: 409,
        code: 'DUPLICATE_EMAIL',
        detail: 'Email is already in use.',
        fieldErrors: [{ field: 'email', message: 'Email is already in use.' }],
      }),
    );
    renderWithAuth(
      <Routes>
        <Route path="/consultants/new" element={<ConsultantFormPage />} />
      </Routes>,
      { user: userWithRole('ADMIN'), route: '/consultants/new' },
    );
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/first name/i), 'A');
    await user.type(screen.getByLabelText(/last name/i), 'B');
    await user.type(screen.getByLabelText(/^email/i), 'a@b.com');
    await user.click(screen.getByRole('button', { name: /save/i }));
    await waitFor(() =>
      expect(screen.getByLabelText(/^email/i)).toHaveAccessibleDescription('Email is already in use.'),
    );
  });
});
