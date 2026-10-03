import type { ReactNode } from 'react';
import { Link } from 'react-router';
import type { PageResponse } from '../api/types';
import { useUrlFilters } from '../hooks/useUrlFilters';
import EmptyState from './EmptyState';
import ErrorBanner from './ErrorBanner';
import LoadingState from './LoadingState';

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  /** API sort field (must be allowlisted by the list endpoint). */
  sortKey?: string;
  numeric?: boolean;
}

interface Props<T> {
  columns: Column<T>[];
  page: PageResponse<T> | null;
  rowKey: (row: T) => string | number;
  /** When set, the first column links to this path. */
  rowLink?: (row: T) => string;
  loading?: boolean;
  error?: unknown;
  emptyMessage?: string;
  /** Embedded tables (panels) don't drive URL paging/sort. */
  paged?: boolean;
}

/** Server-paged list table; sort and page are kept in the URL through useUrlFilters. */
export default function DataTable<T>({
  columns,
  page,
  rowKey,
  rowLink,
  loading = false,
  error,
  emptyMessage,
  paged = true,
}: Props<T>) {
  const filters = useUrlFilters();
  const sort = filters.get('sort') ?? '';
  const [sortField, sortDir] = sort.split(',');

  if (error) return <ErrorBanner error={error} />;
  if (!page && loading) return <LoadingState />;
  if (!page) return null;
  if (page.items.length === 0) return <EmptyState message={emptyMessage} />;

  const toggleSort = (key: string) => {
    const dir = sortField === key && sortDir !== 'desc' ? 'desc' : 'asc';
    filters.set({ sort: `${key},${dir}` });
  };

  return (
    <div className="table-wrap" aria-busy={loading}>
      <table>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} className={col.numeric ? 'num' : undefined} scope="col">
                {paged && col.sortKey ? (
                  <button type="button" className="sort" onClick={() => toggleSort(col.sortKey!)}>
                    {col.header}
                    {sortField === col.sortKey ? (sortDir === 'desc' ? ' ▼' : ' ▲') : ''}
                  </button>
                ) : (
                  col.header
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {page.items.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((col, i) => (
                <td key={col.key} className={col.numeric ? 'num' : undefined}>
                  {i === 0 && rowLink ? <Link to={rowLink(row)}>{col.render(row)}</Link> : col.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {paged && (
        <div className="pager">
          <span>
            {page.totalItems} record{page.totalItems === 1 ? '' : 's'} · page {page.page + 1} of{' '}
            {Math.max(page.totalPages, 1)}
          </span>
          <span className="page__actions">
            <button
              type="button"
              disabled={page.page <= 0}
              onClick={() => filters.set({ page: String(page.page - 1) })}
            >
              Previous
            </button>
            <button
              type="button"
              disabled={page.page + 1 >= page.totalPages}
              onClick={() => filters.set({ page: String(page.page + 1) })}
            >
              Next
            </button>
          </span>
        </div>
      )}
    </div>
  );
}

/** Wraps a plain array as a single page for embedded tables. */
export function asPage<T>(items: T[]): PageResponse<T> {
  return { items, page: 0, size: items.length, totalItems: items.length, totalPages: 1 };
}
