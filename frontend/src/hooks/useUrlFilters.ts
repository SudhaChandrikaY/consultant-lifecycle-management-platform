import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';

export type FilterValue = string | string[] | null | undefined;

/**
 * List filters, paging, and sort live in the URL query string with the same names as the list
 * API, so dashboard and report links open pre-filtered lists (FR-083, FR-092). Repeated keys
 * (status=A&status=B) are multi-values.
 */
export function useUrlFilters() {
  const [params, setParams] = useSearchParams();

  const get = useCallback((key: string) => params.get(key) ?? undefined, [params]);
  const getAll = useCallback((key: string) => params.getAll(key), [params]);

  /** Applies updates; any change other than `page` resets paging to the first page. */
  const set = useCallback(
    (updates: Record<string, FilterValue>) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          for (const [key, value] of Object.entries(updates)) {
            next.delete(key);
            if (Array.isArray(value)) value.filter(Boolean).forEach((v) => next.append(key, v));
            else if (value !== null && value !== undefined && value !== '') next.set(key, value);
          }
          if (!('page' in updates)) next.delete('page');
          return next;
        },
        { replace: true },
      );
    },
    [setParams],
  );

  const clear = useCallback(() => setParams(new URLSearchParams(), { replace: true }), [setParams]);

  /** All current params as a query object for the API client (arrays for repeated keys). */
  const query = useMemo(() => {
    const out: Record<string, string | string[]> = {};
    for (const key of new Set(params.keys())) {
      const values = params.getAll(key);
      out[key] = values.length > 1 ? values : values[0];
    }
    return out;
  }, [params]);

  return { params, get, getAll, set, clear, query, queryString: params.toString() };
}
