import { Link } from 'react-router';
import { reportsApi } from '../../api/reports';
import type { CountLink, SubmissionStatus, VendorClientRow } from '../../api/types';
import EmptyState from '../../components/EmptyState';
import ErrorBanner from '../../components/ErrorBanner';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';
import { useUrlFilters } from '../../hooks/useUrlFilters';
import { CONSULTANT_STATUS_LABELS, formatDate, SUBMISSION_STATUS_LABELS } from '../../labels';

const REPORTS = [
  { key: 'submissions', label: 'Submissions by Recruiter', ranged: true },
  { key: 'placements', label: 'Placements by Recruiter', ranged: true },
  { key: 'pipeline', label: 'Consultant Pipeline', ranged: false },
  { key: 'bench-ready', label: 'Bench and Ready', ranged: false },
  { key: 'activity', label: 'Vendor/Client Activity', ranged: true },
] as const;

type ReportKey = (typeof REPORTS)[number]['key'];

const NO_DATA = 'No data for this period';

/** A figure that drills down into the list reproducing it (FR-092). */
function Cell({ value, link }: { value: number; link?: CountLink }) {
  if (!link || value === 0) return <>{value}</>;
  return <Link to={`/${link.list}?${link.query}`}>{value}</Link>;
}

/** Report picker, date range (default: current month), and one table per report (US8). */
export default function ReportsPage() {
  const filters = useUrlFilters();
  const active = (filters.get('report') as ReportKey | undefined) ?? 'submissions';
  const meta = REPORTS.find((r) => r.key === active) ?? REPORTS[0];
  const range = { from: filters.get('from'), to: filters.get('to') };

  return (
    <PageLayout title="Reports">
      <div className="tabs" role="tablist">
        {REPORTS.map((r) => (
          <button
            key={r.key}
            type="button"
            role="tab"
            aria-selected={r.key === active}
            onClick={() => filters.set({ report: r.key, from: range.from ?? null, to: range.to ?? null })}
          >
            {r.label}
          </button>
        ))}
      </div>
      {meta.ranged && (
        <div className="filter-bar">
          <div className="field">
            <label htmlFor="report-from">From</label>
            <input id="report-from" type="date" value={range.from ?? ''} onChange={(e) => filters.set({ report: active, from: e.target.value, to: range.to ?? null })} />
          </div>
          <div className="field">
            <label htmlFor="report-to">To</label>
            <input id="report-to" type="date" value={range.to ?? ''} onChange={(e) => filters.set({ report: active, from: range.from ?? null, to: e.target.value })} />
          </div>
          <span className="muted small">Leave empty for the current month.</span>
        </div>
      )}
      <section className="card" role="tabpanel" aria-label={meta.label}>
        {active === 'submissions' && <SubmissionsByRecruiter from={range.from} to={range.to} />}
        {active === 'placements' && <PlacementsByRecruiter from={range.from} to={range.to} />}
        {active === 'pipeline' && <Pipeline />}
        {active === 'bench-ready' && <BenchReady />}
        {active === 'activity' && <VendorClientActivity from={range.from} to={range.to} />}
      </section>
    </PageLayout>
  );
}

function RangeCaption({ from, to }: { from: string; to: string }) {
  return (
    <p className="muted">
      {formatDate(from)} – {formatDate(to)}
    </p>
  );
}

const SUBMISSION_COLUMNS: SubmissionStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'INTERVIEW_SCHEDULED',
  'INTERVIEW_CLEARED',
  'OFFER',
  'PLACED',
  'REJECTED',
  'WITHDRAWN',
];

function SubmissionsByRecruiter({ from, to }: { from?: string; to?: string }) {
  const report = useApi(() => reportsApi.submissionsByRecruiter({ from, to }), [from, to]);
  if (report.error) return <ErrorBanner error={report.error} />;
  if (!report.data) return <LoadingState />;
  const r = report.data;
  return (
    <>
      <RangeCaption from={r.from} to={r.to} />
      {r.empty ? (
        <EmptyState message={NO_DATA} />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Recruiter</th>
                <th scope="col" className="num">Total</th>
                {SUBMISSION_COLUMNS.map((s) => (
                  <th key={s} scope="col" className="num">
                    {SUBMISSION_STATUS_LABELS[s]}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {r.rows.map((row) => (
                <tr key={row.recruiterId}>
                  <td>{row.recruiterName}</td>
                  <td className="num">
                    <Cell value={row.total} link={row.links.total} />
                  </td>
                  {SUBMISSION_COLUMNS.map((s) => (
                    <td key={s} className="num">
                      <Cell value={row.byStatus[s] ?? 0} link={row.links[s]} />
                    </td>
                  ))}
                </tr>
              ))}
              <tr>
                <th scope="row">Total</th>
                <td className="num">
                  <strong>{r.totals.total}</strong>
                </td>
                {SUBMISSION_COLUMNS.map((s) => (
                  <td key={s} className="num">
                    {r.totals.byStatus[s] ?? 0}
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      )}
      <p className="muted small">Counted by submitted date in the range; columns show each submission’s current status.</p>
    </>
  );
}

function PlacementsByRecruiter({ from, to }: { from?: string; to?: string }) {
  const report = useApi(() => reportsApi.placementsByRecruiter({ from, to }), [from, to]);
  if (report.error) return <ErrorBanner error={report.error} />;
  if (!report.data) return <LoadingState />;
  const r = report.data;
  return (
    <>
      <RangeCaption from={r.from} to={r.to} />
      {r.empty ? (
        <EmptyState message={NO_DATA} />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Recruiter</th>
                <th scope="col" className="num">Placements</th>
              </tr>
            </thead>
            <tbody>
              {r.rows.map((row) => (
                <tr key={row.recruiterId}>
                  <td>{row.recruiterName}</td>
                  <td className="num">
                    <Cell value={row.placements} link={row.links.placements} />
                  </td>
                </tr>
              ))}
              <tr>
                <th scope="row">Total</th>
                <td className="num">
                  <strong>{r.totals.placements}</strong>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
      <p className="muted small">Counted by placement creation date.</p>
    </>
  );
}

function Pipeline() {
  const report = useApi(() => reportsApi.consultantPipeline(), []);
  if (report.error) return <ErrorBanner error={report.error} />;
  if (!report.data) return <LoadingState />;
  const r = report.data;
  if (r.empty) return <EmptyState message="No consultants yet." />;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th scope="col">Status</th>
            <th scope="col" className="num">Consultants</th>
          </tr>
        </thead>
        <tbody>
          {r.rows.map((row) => (
            <tr key={row.status}>
              <td>{CONSULTANT_STATUS_LABELS[row.status]}</td>
              <td className="num">
                <Cell value={row.count} link={row.links.count} />
              </td>
            </tr>
          ))}
          <tr>
            <th scope="row">Total</th>
            <td className="num">
              <strong>{r.total}</strong>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

function BenchReady() {
  const report = useApi(() => reportsApi.benchReady(), []);
  if (report.error) return <ErrorBanner error={report.error} />;
  if (!report.data) return <LoadingState />;
  const r = report.data;
  return (
    <>
      <p className="muted">As of {formatDate(r.asOf)}</p>
      <div className="tiles">
        <Link className="tile" to={`/${r.links.bench.list}?${r.links.bench.query}`}>
          <div className="tile__value">{r.bench}</div>
          <div className="tile__label">Bench</div>
        </Link>
        <Link className="tile" to={`/${r.links.ready.list}?${r.links.ready.query}`}>
          <div className="tile__value">{r.ready}</div>
          <div className="tile__label">Ready</div>
        </Link>
      </div>
    </>
  );
}

function ActivityTable({ title, rows }: { title: string; rows: VendorClientRow[] }) {
  return (
    <>
      <h3>{title}</h3>
      {rows.length === 0 ? (
        <EmptyState message={NO_DATA} />
      ) : (
        <div className="table-wrap" style={{ marginBottom: 16 }}>
          <table>
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col" className="num">Submissions</th>
                <th scope="col" className="num">Interviews scheduled</th>
                <th scope="col" className="num">Placements</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.name}</td>
                  <td className="num">
                    <Cell value={row.submissions} link={row.links.submissions} />
                  </td>
                  <td className="num">
                    <Cell value={row.interviewsScheduled} link={row.links.interviewsScheduled} />
                  </td>
                  <td className="num">
                    <Cell value={row.placements} link={row.links.placements} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function VendorClientActivity({ from, to }: { from?: string; to?: string }) {
  const report = useApi(() => reportsApi.vendorClientActivity({ from, to }), [from, to]);
  if (report.error) return <ErrorBanner error={report.error} />;
  if (!report.data) return <LoadingState />;
  const r = report.data;
  return (
    <>
      <RangeCaption from={r.from} to={r.to} />
      {r.empty ? (
        <EmptyState message={NO_DATA} />
      ) : (
        <>
          <ActivityTable title="Vendors" rows={r.vendors} />
          <ActivityTable title="Clients" rows={r.clients} />
        </>
      )}
      <p className="muted small">
        Submissions and interviews scheduled count by submitted date (interviews = submissions currently in Interview
        Scheduled); placements count by creation date.
      </p>
    </>
  );
}
