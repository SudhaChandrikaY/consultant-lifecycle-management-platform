import { marketingApi } from '../../api/marketing';
import { referenceApi } from '../../api/reference';
import type { MarketingListItem, MarketingStatus } from '../../api/types';
import DataTable, { type Column } from '../../components/DataTable';
import FilterBar from '../../components/FilterBar';
import PageLayout from '../../components/PageLayout';
import StatusBadge, { Badge } from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import { useUrlFilters } from '../../hooks/useUrlFilters';
import { formatDate, MARKETING_STATUS_LABELS } from '../../labels';
import RecruiterFilterSelect from '../consultants/RecruiterFilterSelect';

const STATUSES: MarketingStatus[] = ['DRAFT', 'ACTIVE', 'HOLD', 'CLOSED'];

const COLUMNS: Column<MarketingListItem>[] = [
  { key: 'consultant', header: 'Consultant', render: (m) => m.consultant.fullName },
  { key: 'owner', header: 'Owner recruiter', render: (m) => m.ownerRecruiter.fullName },
  { key: 'team', header: 'Team', render: (m) => m.team.name },
  { key: 'start', header: 'Start date', sortKey: 'startDate', render: (m) => formatDate(m.startDate) },
  { key: 'target', header: 'Target date', sortKey: 'targetDate', render: (m) => formatDate(m.targetDate) },
  {
    key: 'status',
    header: 'Status',
    sortKey: 'status',
    render: (m) => (
      <>
        <StatusBadge status={m.status} label={MARKETING_STATUS_LABELS[m.status]} />{' '}
        {m.overdue && <Badge tone="danger">Overdue</Badge>}
      </>
    ),
  },
];

export default function MarketingListPage() {
  const filters = useUrlFilters();
  const reference = useApi(() => referenceApi.get(), []);
  const list = useApi(() => marketingApi.list(filters.query), [filters.queryString]);

  return (
    <PageLayout title="Marketing">
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
                {MARKETING_STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
        <RecruiterFilterSelect value={filters.get('recruiterId') ?? ''} onChange={(recruiterId) => filters.set({ recruiterId })} />
        <div className="field">
          <label htmlFor="filter-team">Team</label>
          <select id="filter-team" value={filters.get('teamId') ?? ''} onChange={(e) => filters.set({ teamId: e.target.value })}>
            <option value="">Any</option>
            {(reference.data?.teams ?? []).map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filter-overdue">
            <input
              id="filter-overdue"
              type="checkbox"
              checked={filters.get('overdue') === 'true'}
              onChange={(e) => filters.set({ overdue: e.target.checked ? 'true' : null })}
            />{' '}
            Overdue only
          </label>
        </div>
      </FilterBar>
      <DataTable
        columns={COLUMNS}
        page={list.data}
        loading={list.loading}
        error={list.error}
        rowKey={(m) => m.id}
        rowLink={(m) => `/marketing/${m.id}`}
        emptyMessage="No marketing assignments match these filters."
      />
    </PageLayout>
  );
}
