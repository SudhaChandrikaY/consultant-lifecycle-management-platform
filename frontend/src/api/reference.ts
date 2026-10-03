import { client } from './client';
import type { ReferenceData } from './types';

let cached: Promise<ReferenceData> | null = null;

/** GET /api/reference, cached in module memory for the session. */
export const referenceApi = {
  get: (): Promise<ReferenceData> => {
    if (!cached) {
      cached = client.get<ReferenceData>('/api/reference').catch((err: unknown) => {
        cached = null;
        throw err;
      });
    }
    return cached;
  },
  /** Test hook. */
  clearCache: () => {
    cached = null;
  },
};
