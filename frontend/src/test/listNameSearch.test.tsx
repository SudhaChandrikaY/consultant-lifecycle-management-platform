import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useLocation } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { client } from '../api/client';
import ConsultantListPage from '../pages/consultants/ConsultantListPage';
import RecruiterListPage from '../pages/recruiters/RecruiterListPage';
import { REFERENCE } from './fixtures';
import { renderWithAuth, userWithRole } from './renderWithAuth';

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>();
  return { ...actual, client: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() } };
});

const mocked = vi.mocked(client);
const EMPTY_PAGE = { items: [], page: 0, size: 25, totalItems: 0, totalPages: 0 };

/** Shows the current query string so tests can assert what reached the URL. */
function LocationProbe() {
  return <output data-testid="location">{useLocation().search}</output>;
}

function render(path: string, element: React.ReactElement, route: string) {
  return renderWithAuth(
    <Routes>
      <Route
        path={path}
        element={
          <>
            {element}
            <LocationProbe />
          </>
        }
      />
    </Routes>,
    { user: userWithRole('ADMIN'), route },
  );
}

const search = () => new URLSearchParams(screen.getByTestId('location').textContent ?? '');

/** Query objects the list endpoint was called with, most recent last. */
function listCalls(path: string) {
  return mocked.get.mock.calls.filter(([p]) => p === path).map(([, q]) => q as Record<string, unknown>);
}

beforeEach(() => {
  vi.resetAllMocks();
  mocked.get.mockImplementation(async (path: string) => {
    if (path === '/api/reference') return REFERENCE;
    if (path === '/api/consultants/skills') return ['Java', 'Go'];
    return EMPTY_PAGE;
  });
});

describe.each([
  { name: 'Consultants', path: '/consultants', api: '/api/consultants', element: <ConsultantListPage /> },
  { name: 'Recruiters', path: '/recruiters', api: '/api/recruiters', element: <RecruiterListPage /> },
])('$name name search', ({ path, api, element }) => {
  it('keeps typed characters visible while typing', async () => {
    render(path, element, path);
    const input = await screen.findByLabelText(/search name/i);
    const user = userEvent.setup();
    await user.type(input, 'Riya Pat');
    expect(input).toHaveValue('Riya Pat');
    // Typing alone does not filter yet.
    expect(search().get('q')).toBeNull();
  });

  it('applies the search on Enter and keeps existing filters', async () => {
    render(path, element, `${path}?status=ACTIVE&page=2`);
    const input = await screen.findByLabelText(/search name/i);
    const user = userEvent.setup();
    await user.type(input, 'riya{Enter}');

    await waitFor(() => expect(search().get('q')).toBe('riya'));
    expect(search().get('status')).toBe('ACTIVE');
    expect(search().get('page')).toBeNull(); // a new search starts from the first page
    expect(input).toHaveValue('riya');
    await waitFor(() => expect(listCalls(api).at(-1)).toMatchObject({ q: 'riya', status: 'ACTIVE' }));
  });

  it('shows the search from the URL and empties it on Clear filters', async () => {
    render(path, element, `${path}?q=marcus&status=ACTIVE`);
    const input = await screen.findByLabelText(/search name/i);
    expect(input).toHaveValue('marcus');

    await userEvent.setup().click(screen.getByRole('button', { name: /clear filters/i }));
    await waitFor(() => expect(input).toHaveValue(''));
    expect(search().toString()).toBe('');
  });

  it('clearing the text and pressing Enter removes the search', async () => {
    render(path, element, `${path}?q=marcus&status=ACTIVE`);
    const input = await screen.findByLabelText(/search name/i);
    const user = userEvent.setup();
    await user.clear(input);
    await user.type(input, '{Enter}');
    await waitFor(() => expect(search().get('q')).toBeNull());
    expect(search().get('status')).toBe('ACTIVE');
  });
});
