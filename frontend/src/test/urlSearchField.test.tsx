import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useLocation, useNavigate } from 'react-router';
import { describe, expect, it } from 'vitest';
import UrlSearchField from '../components/UrlSearchField';
import { renderWithAuth, userWithRole } from './renderWithAuth';

function Harness() {
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <>
      <UrlSearchField label="Search name" />
      <button type="button" onClick={() => navigate('/list?q=linked&teamId=2')}>
        Follow link
      </button>
      <output data-testid="location">{location.search}</output>
    </>
  );
}

function setup(route: string) {
  renderWithAuth(
    <Routes>
      <Route path="/list" element={<Harness />} />
    </Routes>,
    { user: userWithRole('ADMIN'), route },
  );
  return { input: screen.getByLabelText(/search name/i), user: userEvent.setup() };
}

const params = () => new URLSearchParams(screen.getByTestId('location').textContent ?? '');

describe('UrlSearchField', () => {
  it('applies on blur, trims, and keeps other params', async () => {
    const { input, user } = setup('/list?teamId=1&page=3');
    await user.type(input, '  Ada  ');
    expect(input).toHaveValue('  Ada  ');
    await user.tab();
    await waitFor(() => expect(params().get('q')).toBe('Ada'));
    expect(params().get('teamId')).toBe('1');
    expect(params().get('page')).toBeNull();
  });

  it('does not touch the URL when the text is unchanged', async () => {
    const { input, user } = setup('/list?q=ada&page=3');
    await user.click(input);
    await user.tab();
    expect(params().get('page')).toBe('3');
    await user.type(input, '{Enter}');
    expect(params().get('page')).toBe('3');
  });

  it('follows URL changes made elsewhere, such as navigation links', async () => {
    const { input, user } = setup('/list?q=old');
    expect(input).toHaveValue('old');
    await user.click(screen.getByRole('button', { name: /follow link/i }));
    await waitFor(() => expect(input).toHaveValue('linked'));
  });
});
