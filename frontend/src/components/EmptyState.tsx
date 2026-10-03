import { useAuth } from '../auth/AuthProvider';

export const UNLINKED_RECRUITER_MESSAGE = 'Your account is not linked to a recruiter profile. Contact an Admin.';

/**
 * Shared empty state. A RECRUITER with no linked recruiter profile always sees the "contact an
 * Admin" message instead of a generic one (spec edge case, ui-routes cross-cutting).
 */
export default function EmptyState({ message = 'No records found.' }: { message?: string }) {
  const { user } = useAuth();
  const unlinked = user?.role === 'RECRUITER' && user.recruiterId === null;
  return <div className="state">{unlinked ? UNLINKED_RECRUITER_MESSAGE : message}</div>;
}
