import { useAuth } from '../../auth/AuthProvider';
import { ROLE_LABELS } from '../../auth/permissions';
import PageLayout from '../../components/PageLayout';

/** Welcome shell; replaced by the role-shaped dashboard in US7. */
export default function DashboardPage() {
  const { user } = useAuth();
  return (
    <PageLayout title="Dashboard">
      <div className="card">
        <p>
          Welcome, {user?.displayName}. You are signed in as {user ? ROLE_LABELS[user.role] : ''}.
        </p>
      </div>
    </PageLayout>
  );
}
