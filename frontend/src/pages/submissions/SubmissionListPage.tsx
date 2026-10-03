import { submissionsApi } from '../../api/submissions';
import type { SubmissionListItem, SubmissionStatus } from '../../api/types';
import DataTable, { type Column } from '../../components/DataTable';
import FilterBar from '../../components/FilterBar';
import PageLayout from '../../components/PageLayout';
import StatusBadge from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import { useUrlFilters } from '../../hooks/useUrlFilters';
import { formatDate, formatMoney, SUBMISSION_STATUS_LABELS } from '../../labels';
import RecruiterFilterSelect from '../consultants/RecruiterFilterSelect';
import CounterpartyFilter from './CounterpartyFilter';

const STATUSES = Object.keys(SUBMISSION_STATUS_LABELS) as SubmissionStatus[];

const COLUMNS: Column<SubmissionListItem>[] = [
  { key: 'job', header: 'Job title', render: (s) => s.jobTitle },
  { key: 'consultant', header: 'Consultant', render: (s) => s.consultant.fullName },
  { key: 'recruiter', header: 'Recruiter', render: (s) => s.recruiter.fullName },
  { key: 'vendor', header: 'Vendor', render: (s) => s.vendor.name },
  { key: 'client', header: 'Client', render: (s) => s.client.name },
  { key: 'submitted', header: 'Submitted', sortKey: 'submittedDate', render: (s) => formatDate(s.submittedDate) },
  { key: 'rate', header: 'Bill rate (USD/hr)', sortKey: 'billRate', numeric: true, render: (s) => formatMoney(s.billRate) },
  {
    key: 'status',
    header: 'Status',
    sortKey: 'status',
    render: (s) => <StatusBadge status={s.status} label={SUBMISSION_STATUS_LABELS[s.status]} />,
  },
];

export default function SubmissionListPage() {
  const filters = useUrlFilters();
  const list = useApi(() => submissionsApi.list(filters.query), [filters.queryString]);

  return (
    <PageLayout title="Submissions">
      <FilterBar onClear={filters.clear}>
        <div className="field">
          <label htmlFor="filter-status">Status</label>
          <select
            id="filter-status"
            multiple
            value={filters.getAll('status')}
            onChange={(e) => filters.set({ status: Array.from(e.target.selectedOptions, (o) => o.value) })}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {SUBMISSION_STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
        <RecruiterFilterSelect value={filters.get('recruiterId') ?? ''} onChange={(recruiterId) => filters.set({ recruiterId })} />
        <CounterpartyFilter kind="vendors" label="Vendor" param="vendorId" />
        <CounterpartyFilter kind="clients" label="Client" param="clientId" />
        <div className="field">
          <label htmlFor="filter-from">Submitted from</label>
          <input id="filter-from" type="date" value={filters.get('submittedFrom') ?? ''} onChange={(e) => filters.set({ submittedFrom: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="filter-to">Submitted to</label>
          <input id="filter-to" type="date" value={filters.get('submittedTo') ?? ''} onChange={(e) => filters.set({ submittedTo: e.target.value })} />
        </div>
      </FilterBar>
      <DataTable
        columns={COLUMNS}
        page={list.data}
        loading={list.loading}
        error={list.error}
        rowKey={(s) => s.id}
        rowLink={(s) => `/submissions/${s.id}`}
        emptyMessage="No submissions match these filters."
      />
    </PageLayout>
  );
}
