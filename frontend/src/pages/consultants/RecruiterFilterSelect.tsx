import { recruitersApi } from '../../api/recruiters';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import { useApi } from '../../hooks/useApi';

/**
 * Recruiter filter for the consultant list. Only ADMIN and MANAGER may list recruiters; for
 * other roles an id arriving from a dashboard link is still honored.
 */
export default function RecruiterFilterSelect({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const { user } = useAuth();
  const allowed = can.viewRecruiters(user?.role);
  const recruiters = useApi(() => (allowed ? recruitersApi.all() : Promise.resolve(null)), [allowed]);
  if (!allowed && !value) return null;
  const options = recruiters.data?.items ?? [];
  return (
    <div className="field">
      <label htmlFor="filter-recruiter">Recruiter</label>
      <select id="filter-recruiter" value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Any</option>
        {options.map((r) => (
          <option key={r.id} value={r.id}>
            {r.fullName}
            {r.status === 'INACTIVE' ? ' (inactive)' : ''}
          </option>
        ))}
        {value && !options.some((r) => String(r.id) === value) && <option value={value}>Recruiter #{value}</option>}
      </select>
    </div>
  );
}
