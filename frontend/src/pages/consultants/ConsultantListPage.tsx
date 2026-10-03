import { Link } from 'react-router';
import { consultantsApi } from '../../api/consultants';
import { referenceApi } from '../../api/reference';
import type { ConsultantListItem, ConsultantStatus, VisaType } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import DataTable, { type Column } from '../../components/DataTable';
import FilterBar from '../../components/FilterBar';
import PageLayout from '../../components/PageLayout';
import UrlSearchField from '../../components/UrlSearchField';
import StatusBadge, { Badge } from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import { useUrlFilters } from '../../hooks/useUrlFilters';
import { CONSULTANT_STATUS_LABELS, label, VISA_TYPE_LABELS } from '../../labels';
import RecruiterFilterSelect from './RecruiterFilterSelect';

const COLUMNS: Column<ConsultantListItem>[] = [
  { key: 'name', header: 'Name', sortKey: 'lastName', render: (c) => c.fullName },
  { key: 'skill', header: 'Primary skill', sortKey: 'primarySkill', render: (c) => c.primarySkill ?? '—' },
  {
    key: 'years',
    header: 'Years',
    sortKey: 'yearsExperience',
    numeric: true,
    render: (c) => c.yearsExperience ?? '—',
  },
  { key: 'visa', header: 'Visa type', render: (c) => label(VISA_TYPE_LABELS, c.visaType) },
  { key: 'recruiter', header: 'Assigned recruiter', render: (c) => c.assignedRecruiter?.fullName ?? 'Unassigned' },
  {
    key: 'status',
    header: 'Status',
    sortKey: 'status',
    render: (c) => (
      <>
        <StatusBadge status={c.status} />{' '}
        {c.needsReassignment && <Badge tone="danger">Needs reassignment</Badge>}
      </>
    ),
  },
];

/** Consultant list. Never shows email or phone (FR-024). */
export default function ConsultantListPage() {
  const { user } = useAuth();
  const filters = useUrlFilters();
  const reference = useApi(() => referenceApi.get(), []);
  const skills = useApi(() => consultantsApi.skills(), []);
  const list = useApi(() => consultantsApi.list(filters.query), [filters.queryString]);

  const statuses = filters.getAll('status');

  return (
    <PageLayout
      title="Consultants"
      actions={
        can.editConsultant(user?.role) && (
          <Link className="button primary" to="/consultants/new">
            Add consultant
          </Link>
        )
      }
    >
      <FilterBar onClear={filters.clear}>
        <UrlSearchField label="Search name" />
        <div className="field">
          <label htmlFor="filter-status">Status</label>
          <select
            id="filter-status"
            multiple
            value={statuses}
            onChange={(e) => filters.set({ status: Array.from(e.target.selectedOptions, (o) => o.value) })}
          >
            {(reference.data?.consultantStatuses ?? []).map((s: ConsultantStatus) => (
              <option key={s} value={s}>
                {CONSULTANT_STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filter-skill">Primary skill</label>
          <select
            id="filter-skill"
            value={filters.get('primarySkill') ?? ''}
            onChange={(e) => filters.set({ primarySkill: e.target.value })}
          >
            <option value="">Any</option>
            {(skills.data ?? []).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filter-visa">Visa type</label>
          <select
            id="filter-visa"
            value={filters.get('visaType') ?? ''}
            onChange={(e) => filters.set({ visaType: e.target.value })}
          >
            <option value="">Any</option>
            {(reference.data?.visaTypes ?? []).map((v: VisaType) => (
              <option key={v} value={v}>
                {VISA_TYPE_LABELS[v]}
              </option>
            ))}
          </select>
        </div>
        <RecruiterFilterSelect
          value={filters.get('recruiterId') ?? ''}
          onChange={(recruiterId) => filters.set({ recruiterId })}
        />
        <div className="field">
          <label htmlFor="filter-reassign">
            <input
              id="filter-reassign"
              type="checkbox"
              checked={filters.get('needsReassignment') === 'true'}
              onChange={(e) => filters.set({ needsReassignment: e.target.checked ? 'true' : null })}
            />{' '}
            Needs reassignment
          </label>
        </div>
      </FilterBar>
      <DataTable
        columns={COLUMNS}
        page={list.data}
        loading={list.loading}
        error={list.error}
        rowKey={(c) => c.id}
        rowLink={(c) => `/consultants/${c.id}`}
        emptyMessage="No consultants match these filters."
      />
    </PageLayout>
  );
}
