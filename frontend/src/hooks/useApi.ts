import { useCallback, useEffect, useRef, useState } from 'react';

export interface ApiState<T> {
  data: T | null;
  error: unknown;
  loading: boolean;
  reload: () => void;
  setData: (data: T) => void;
}

/**
 * Runs `loader` on mount and whenever `deps` change, tracking loading/error/data. Stale responses
 * from superseded calls are ignored.
 */
export function useApi<T>(loader: () => Promise<T>, deps: unknown[]): ApiState<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [tick, setTick] = useState(0);
  const callId = useRef(0);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  useEffect(() => {
    const id = ++callId.current;
    setLoading(true);
    setError(null);
    loaderRef
      .current()
      .then((result) => {
        if (id === callId.current) setData(result);
      })
      .catch((err: unknown) => {
        if (id === callId.current) setError(err);
      })
      .finally(() => {
        if (id === callId.current) setLoading(false);
      });
  }, [...deps, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { data, error, loading, reload, setData };
}
