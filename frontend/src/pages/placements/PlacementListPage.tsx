import { placementsApi } from '../../api/placements';
import type { PlacementListItem } from '../../api/types';
import DataTable, { type Column } from '../../components/DataTable';
import FilterBar from '../../components/FilterBar';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';
import { useUrlFilters } from '../../hooks/useUrlFilters';
import { formatDate, formatMoney } from '../../labels';
import RecruiterFilterSelect from '../consultants/RecruiterFilterSelect';
import CounterpartyFilter from '../submissions/CounterpartyFilter';

const COLUMNS: Column<PlacementListItem>[] = [
  { key: 'consultant', header: 'Consultant', render: (p) => p.consultant.fullName },
  { key: 'recruiter', header: 'Recruiter', render: (p) => p.recruiter.fullName },
  { key: 'client', header: 'Client', render: (p) => p.client.name },
  { key: 'vendor', header: 'Vendor', render: (p) => p.vendor.name },
  { key: 'start', header: 'Start date', sortKey: 'startDate', render: (p) => formatDate(p.startDate) },
  { key: 'rate', header: 'Bill rate', numeric: true, render: (p) => formatMoney(p.billRate) },
  { key: 'term', header: 'Term (months)', numeric: true, render: (p) => p.contractTermMonths },
  { key: 'end', header: 'Expected end', render: (p) => formatDate(p.expectedEndDate) },
];

export default function PlacementListPage() {
  const filters = useUrlFilters();
  const list = useApi(() => placementsApi.list(filters.query), [filters.queryString]);
  const createdFrom = filters.get('createdFrom');
  const createdTo = filters.get('createdTo');

  return (
    <PageLayout title="Placements">
      <FilterBar onClear={filters.clear}>
        <RecruiterFilterSelect value={filters.get('recruiterId') ?? ''} onChange={(recruiterId) => filters.set({ recruiterId })} />
        <CounterpartyFilter kind="clients" label="Client" param="clientId" />
        <CounterpartyFilter kind="vendors" label="Vendor" param="vendorId" />
        <div className="field">
          <label htmlFor="filter-start-from">Start from</label>
          <input id="filter-start-from" type="date" value={filters.get('startFrom') ?? ''} onChange={(e) => filters.set({ startFrom: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="filter-start-to">Start to</label>
          <input id="filter-start-to" type="date" value={filters.get('startTo') ?? ''} onChange={(e) => filters.set({ startTo: e.target.value })} />
        </div>
      </FilterBar>
      {(createdFrom || createdTo) && (
        <div className="banner banner--info" role="status">
          Showing placements created {createdFrom ? `from ${formatDate(createdFrom)} ` : ''}
          {createdTo ? `to ${formatDate(createdTo)}` : ''}.{' '}
          <button type="button" className="link" onClick={() => filters.set({ createdFrom: null, createdTo: null })}>
            Clear
          </button>
        </div>
      )}
      <DataTable
        columns={COLUMNS}
        page={list.data}
        loading={list.loading}
        error={list.error}
        rowKey={(p) => p.id}
        rowLink={(p) => `/placements/${p.id}`}
        emptyMessage="No placements match these filters."
      />
    </PageLayout>
  );
}
