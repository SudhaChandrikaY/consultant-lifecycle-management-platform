import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import type { Role } from '../api/types';
import LoadingState from '../components/LoadingState';
import NotAuthorized from '../pages/NotAuthorized';
import { useAuth } from './AuthProvider';

/** Route guard: forbidden roles see NotAuthorized and the page (and its API calls) never mounts. */
export default function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <LoadingState />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (!roles.includes(user.role)) return <NotAuthorized />;
  return <>{children}</>;
}
