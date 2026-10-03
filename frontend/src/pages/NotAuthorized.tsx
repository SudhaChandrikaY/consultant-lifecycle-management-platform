import { Link } from 'react-router';
import PageLayout from '../components/PageLayout';

export default function NotAuthorized() {
  return (
    <PageLayout title="Not authorized">
      <div className="card">
        <p>You don’t have access to this page.</p>
        <Link to="/">Back to the dashboard</Link>
      </div>
    </PageLayout>
  );
}
