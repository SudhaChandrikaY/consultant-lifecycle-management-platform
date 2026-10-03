import { useEffect, useState } from 'react';
import { useUrlFilters } from '../hooks/useUrlFilters';

interface Props {
  label: string;
  /** URL/API query parameter the text is written to. */
  param?: string;
  placeholder?: string;
}

/**
 * Free-text list search. The typed text is a local draft so every keystroke stays visible; it is
 * written to the URL (and so to the list query) on Enter or blur, keeping all other filters. The
 * draft re-syncs only when the URL value itself changes, e.g. Clear filters or a dashboard link.
 */
export default function UrlSearchField({ label, param = 'q', placeholder = 'Name, then Enter' }: Props) {
  const filters = useUrlFilters();
  const urlValue = filters.get(param) ?? '';
  const [draft, setDraft] = useState(urlValue);

  // Depend on the string, not the filters object (a new object every render), or the draft
  // would be reset after each keystroke.
  useEffect(() => setDraft(urlValue), [urlValue]);

  const apply = () => {
    const next = draft.trim();
    if (next !== urlValue) filters.set({ [param]: next || null });
  };

  const id = `filter-${param}`;
  return (
    <form
      className="field"
      onSubmit={(e) => {
        e.preventDefault();
        apply();
      }}
    >
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        type="search"
        value={draft}
        placeholder={placeholder}
        onChange={(e) => setDraft(e.target.value)}
        onBlur={apply}
      />
    </form>
  );
}
