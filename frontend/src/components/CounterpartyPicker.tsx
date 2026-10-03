import { useEffect, useState } from 'react';
import { counterpartiesApi, type CounterpartyKind } from '../api/counterparties';
import type { IdName } from '../api/types';
import type { FieldControlProps } from './FormField';

/** Either an existing record ({id}) or a new name ({name}) to find-or-create (FR-051). */
export interface CounterpartyValue {
  id: number | null;
  name: string;
}

interface Props {
  kind: CounterpartyKind;
  value: CounterpartyValue;
  onChange: (value: CounterpartyValue) => void;
  control: FieldControlProps;
}

/** Typeahead over /api/vendors or /api/clients with an "Add '{typed}'" option. */
export default function CounterpartyPicker({ kind, value, onChange, control }: Props) {
  const [options, setOptions] = useState<IdName[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (value.id !== null || !open) return;
    const handle = setTimeout(() => {
      counterpartiesApi
        .search(kind, value.name.trim())
        .then((found) => setOptions(found ?? []))
        .catch(() => setOptions([]));
    }, 200);
    return () => clearTimeout(handle);
  }, [kind, value.id, value.name, open]);

  const typed = value.name.trim();
  const exact = options.find((o) => o.name.toLowerCase() === typed.toLowerCase());
  const listId = `${control.id}-options`;

  return (
    <div style={{ position: 'relative' }}>
      <input
        {...control}
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        autoComplete="off"
        value={value.name}
        maxLength={120}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        onChange={(e) => onChange({ id: null, name: e.target.value })}
      />
      {value.id !== null && <span className="field__hint">Existing {kind === 'vendors' ? 'vendor' : 'client'}</span>}
      {open && value.id === null && (options.length > 0 || typed) && (
        <ul
          id={listId}
          role="listbox"
          className="card"
          style={{ position: 'absolute', zIndex: 10, left: 0, right: 0, margin: 0, padding: 4, listStyle: 'none' }}
        >
          {options.map((o) => (
            <li key={o.id}>
              <button
                type="button"
                role="option"
                aria-selected={false}
                className="link"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange({ id: o.id, name: o.name });
                  setOpen(false);
                }}
              >
                {o.name}
              </button>
            </li>
          ))}
          {typed && !exact && (
            <li>
              <button
                type="button"
                role="option"
                aria-selected={false}
                className="link"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => setOpen(false)}
              >
                Add “{typed}”
              </button>
            </li>
          )}
        </ul>
      )}
    </div>
  );
}
