import { Link } from 'react-router';
import { dashboardApi } from '../../api/dashboard';
import type { Dashboard } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import ErrorBanner from '../../components/ErrorBanner';
import HistoryList from '../../components/HistoryList';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';
import { CONSULTANT_STATUS_LABELS } from '../../labels';
import CountTile from './CountTile';

/** Role-shaped dashboard (US7): counts link to matching lists; activity is scoped by the API. */
export default function DashboardPage() {
  const { user } = useAuth();
  const dashboard = useApi(() => dashboardApi.get(), []);

  return (
    <PageLayout title="Dashboard">
      {dashboard.error ? (
        <ErrorBanner error={dashboard.error} onReload={dashboard.reload} />
      ) : !dashboard.data ? (
        <LoadingState />
      ) : (
        <DashboardBody data={dashboard.data} greeting={user?.displayName} />
      )}
    </PageLayout>
  );
}

function DashboardBody({ data, greeting }: { data: Dashboard; greeting?: string }) {
  return (
    <>
      <p className="muted">
        Welcome{greeting ? `, ${greeting}` : ''}.{' '}
        {data.scope === 'ORGANIZATION'
          ? 'Figures cover the whole organization.'
          : data.scope === 'OWN'
            ? 'Figures cover your consultants, submissions, and placements.'
            : 'Figures cover the consultant pipeline.'}
      </p>
      {data.message && (
        <div className="banner banner--info" role="status">
          {data.message}
        </div>
      )}
      <div className="tiles">
        {data.counts.map((c) => (
          <CountTile key={c.key} label={c.label} value={c.value} link={c.link} />
        ))}
      </div>

      {data.consultantsByStatus && (
        <section className="card" aria-labelledby="by-status-heading">
          <h2 id="by-status-heading">Consultants by status</h2>
          <div className="tiles">
            {data.consultantsByStatus.map((s) => (
              <CountTile key={s.status} label={CONSULTANT_STATUS_LABELS[s.status]} value={s.count} link={s.link} />
            ))}
          </div>
        </section>
      )}

      {data.recruiterPerformance && (
        <section className="card" aria-labelledby="performance-heading">
          <h2 id="performance-heading">Recruiter performance</h2>
          <RecruiterPerformanceTable rows={data.recruiterPerformance} />
        </section>
      )}

      <section className="card" aria-labelledby="activity-heading">
        <h2 id="activity-heading">Recent activity</h2>
        <HistoryList entries={data.recentActivity} emptyMessage="No recent activity." />
      </section>
    </>
  );
}

function RecruiterPerformanceTable({ rows }: { rows: NonNullable<Dashboard['recruiterPerformance']> }) {
  if (rows.length === 0) return <p className="muted">No active recruiters.</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th scope="col">Recruiter</th>
            <th scope="col" className="num">
              Assigned consultants
            </th>
            <th scope="col" className="num">
              Active submissions
            </th>
            <th scope="col" className="num">
              In interviews
            </th>
            <th scope="col" className="num">
              Placements this month
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.recruiterId}>
              <td>
                <Link to={`/recruiters/${r.recruiterId}`}>{r.recruiterName}</Link>
              </td>
              <td className="num">
                <Link to={`/consultants?recruiterId=${r.recruiterId}`}>{r.assignedConsultants}</Link>
              </td>
              <td className="num">{r.activeSubmissions}</td>
              <td className="num">{r.interviews}</td>
              <td className="num">{r.placementsThisMonth}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
