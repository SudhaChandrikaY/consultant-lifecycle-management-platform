import type { ReactNode } from 'react';
import { problemOf } from '../api/errors';
import { missingItemLabel } from '../labels';

interface Props {
  error: unknown;
  /** Called by the Reload button on CONCURRENT_MODIFICATION; defaults to a page reload. */
  onReload?: () => void;
  children?: ReactNode;
}

/** Form-level error: problem detail, missing readiness items, concurrency and 403 messages. */
export default function ErrorBanner({ error, onReload, children }: Props) {
  if (!error) return null;
  const problem = problemOf(error);

  if (problem?.code === 'CONCURRENT_MODIFICATION') {
    return (
      <div className="banner banner--warning" role="alert">
        This record was changed by someone else. Reload to continue.
        <div className="banner__actions">
          <button type="button" onClick={onReload ?? (() => window.location.reload())}>
            Reload
          </button>
        </div>
      </div>
    );
  }

  if (problem?.status === 403) {
    return (
      <div className="banner banner--error" role="alert">
        Not authorized
      </div>
    );
  }

  const missingItems = Array.isArray(problem?.missingItems) ? (problem.missingItems as string[]) : [];
  const message =
    problem?.detail ?? (error instanceof Error ? error.message : 'Something went wrong. Please try again.');

  return (
    <div className="banner banner--error" role="alert">
      {message}
      {missingItems.length > 0 && (
        <ul>
          {missingItems.map((item) => (
            <li key={item}>{missingItemLabel(item)}</li>
          ))}
        </ul>
      )}
      {children}
    </div>
  );
}
