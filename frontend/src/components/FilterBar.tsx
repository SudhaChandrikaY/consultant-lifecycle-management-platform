import type { ReactNode } from 'react';

/** Row of list filters with a "Clear" action. */
export default function FilterBar({ children, onClear }: { children: ReactNode; onClear?: () => void }) {
  return (
    <div className="filter-bar" role="search">
      {children}
      {onClear && (
        <button type="button" onClick={onClear}>
          Clear filters
        </button>
      )}
    </div>
  );
}
