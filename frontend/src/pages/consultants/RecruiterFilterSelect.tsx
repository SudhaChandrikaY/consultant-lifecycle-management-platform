/**
 * Recruiter filter for the consultant list. Recruiter options need the recruiter API (US3); until
 * then an id arriving from a dashboard link is still honored and shown.
 */
export default function RecruiterFilterSelect({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  if (!value) return null;
  return (
    <div className="field">
      <label htmlFor="filter-recruiter">Recruiter</label>
      <select id="filter-recruiter" value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Any</option>
        <option value={value}>Recruiter #{value}</option>
      </select>
    </div>
  );
}
