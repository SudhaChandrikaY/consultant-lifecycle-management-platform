import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { recruitersApi } from '../../api/recruiters';
import { referenceApi } from '../../api/reference';
import type { RecruiterListItem } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import DataTable, { type Column } from '../../components/DataTable';
import FilterBar from '../../components/FilterBar';
import PageLayout from '../../components/PageLayout';
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
  const [search, setSearch] = useState(filters.get('q') ?? '');
  const reference = useApi(() => referenceApi.get(), []);
  const list = useApi(() => recruitersApi.list(filters.query), [filters.queryString]);

  useEffect(() => setSearch(filters.get('q') ?? ''), [filters]);

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
        <form
          className="field"
          onSubmit={(e) => {
            e.preventDefault();
            filters.set({ q: search.trim() || null });
          }}
        >
          <label htmlFor="filter-q">Search name</label>
          <input
            id="filter-q"
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onBlur={() => search !== (filters.get('q') ?? '') && filters.set({ q: search.trim() || null })}
          />
        </form>
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
