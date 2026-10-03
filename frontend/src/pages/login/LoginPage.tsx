import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router';
import { useAuth } from '../../auth/AuthProvider';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(username, password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err);
      setPassword('');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login">
      <div className="card">
        <h1>Sign in to CLMP</h1>
        {params.get('expired') === '1' && !error && (
          <div className="banner banner--info" role="status">
            Your session expired. Please sign in again.
          </div>
        )}
        <ErrorBanner error={error} />
        <form className="form" onSubmit={onSubmit} noValidate>
          <FormField label="Username" name="username" error={error} required>
            {(p) => (
              <input
                {...p}
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            )}
          </FormField>
          <FormField label="Password" name="password" error={error} required>
            {(p) => (
              <input
                {...p}
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            )}
          </FormField>
          <div className="form-actions">
            <button className="primary" type="submit" disabled={submitting || !username || !password}>
              {submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
