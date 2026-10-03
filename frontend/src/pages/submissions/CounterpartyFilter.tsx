import { counterpartiesApi, type CounterpartyKind } from '../../api/counterparties';
import { useApi } from '../../hooks/useApi';
import { useUrlFilters } from '../../hooks/useUrlFilters';

/** Vendor or client filter select for list pages (the first 20 by name, plus a linked id). */
export default function CounterpartyFilter({ kind, label, param }: { kind: CounterpartyKind; label: string; param: string }) {
  const filters = useUrlFilters();
  const options = useApi(() => counterpartiesApi.search(kind, ''), [kind]);
  const value = filters.get(param) ?? '';
  const items = options.data ?? [];
  return (
    <div className="field">
      <label htmlFor={`filter-${param}`}>{label}</label>
      <select id={`filter-${param}`} value={value} onChange={(e) => filters.set({ [param]: e.target.value })}>
        <option value="">Any</option>
        {items.map((o) => (
          <option key={o.id} value={o.id}>
            {o.name}
          </option>
        ))}
        {value && !items.some((o) => String(o.id) === value) && <option value={value}>#{value}</option>}
      </select>
    </div>
  );
}
