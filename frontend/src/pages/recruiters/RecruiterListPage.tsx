import { Link } from 'react-router';
import { recruitersApi } from '../../api/recruiters';
import { referenceApi } from '../../api/reference';
import type { RecruiterListItem } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import DataTable, { type Column } from '../../components/DataTable';
import FilterBar from '../../components/FilterBar';
import PageLayout from '../../components/PageLayout';
import UrlSearchField from '../../components/UrlSearchField';
import StatusBadge from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import { useUrlFilters } from '../../hooks/useUrlFilters';

const COLUMNS: Column<RecruiterListItem>[] = [
  { key: 'name', header: 'Name', sortKey: 'fullName', render: (r) => r.fullName },
  { key: 'team', header: 'Team', render: (r) => r.team.name },
  { key: 'region', header: 'Region', render: (r) => r.region.name },
  { key: 'status', header: 'Status', sortKey: 'status', render: (r) => <StatusBadge status={r.status} /> },
  {
    key: 'count',
    header: 'Assigned consultants',
    numeric: true,
    render: (r) => <Link to={`/consultants?recruiterId=${r.id}`}>{r.assignedConsultantCount}</Link>,
  },
];

export default function RecruiterListPage() {
  const { user } = useAuth();
  const filters = useUrlFilters();
  const reference = useApi(() => referenceApi.get(), []);
  const list = useApi(() => recruitersApi.list(filters.query), [filters.queryString]);

  return (
    <PageLayout
      title="Recruiters"
      actions={
        can.manageRecruiters(user?.role) && (
          <Link className="button primary" to="/recruiters/new">
            Add recruiter
          </Link>
        )
      }
    >
      <FilterBar onClear={filters.clear}>
        <UrlSearchField label="Search name" />
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
          <label htmlFor="filter-region">Region</label>
          <select
            id="filter-region"
            value={filters.get('regionId') ?? ''}
            onChange={(e) => filters.set({ regionId: e.target.value })}
          >
            <option value="">Any</option>
            {(reference.data?.regions ?? []).map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filter-status">Status</label>
          <select id="filter-status" value={filters.get('status') ?? ''} onChange={(e) => filters.set({ status: e.target.value })}>
            <option value="">Any</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>
      </FilterBar>
      <DataTable
        columns={COLUMNS}
        page={list.data}
        loading={list.loading}
        error={list.error}
        rowKey={(r) => r.id}
        rowLink={(r) => `/recruiters/${r.id}`}
        emptyMessage="No recruiters match these filters."
      />
    </PageLayout>
  );
}
