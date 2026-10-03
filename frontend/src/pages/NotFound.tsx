import { Link } from 'react-router';
import PageLayout from '../components/PageLayout';

export default function NotFound() {
  return (
    <PageLayout title="Page not found">
      <div className="card">
        <p>The page you requested doesn’t exist.</p>
        <Link to="/">Back to the dashboard</Link>
      </div>
    </PageLayout>
  );
}
