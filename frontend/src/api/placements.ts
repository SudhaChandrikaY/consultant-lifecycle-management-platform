import { client, type Query } from './client';
import type { PageResponse, PlacementCreated, PlacementDetail, PlacementDraft, PlacementListItem } from './types';

const BASE = '/api/placements';

export const placementsApi = {
  list: (query: Query) => client.get<PageResponse<PlacementListItem>>(BASE, query),
  get: (id: number | string) => client.get<PlacementDetail>(`${BASE}/${id}`),
  draft: (submissionId: number | string) => client.get<PlacementDraft>(`${BASE}/draft`, { submissionId }),
  create: (body: { submissionId: number; startDate: string; billRate: number | null; contractTermMonths: number | null }) =>
    client.post<PlacementCreated>(BASE, body),
  edit: (
    id: number | string,
    body: { startDate?: string; billRate?: number; contractTermMonths?: number; version: number },
  ) => client.patch<PlacementDetail>(`${BASE}/${id}`, body),
};

/** start + term months, as the server computes it (FR-071). */
export function expectedEndDate(start: string, months: number): string | null {
  if (!start || !Number.isInteger(months) || months < 1) return null;
  const [y, m, d] = start.split('-').map(Number);
  const target = new Date(Date.UTC(y, m - 1 + months, 1));
  const lastDay = new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth() + 1, 0)).getUTCDate();
  target.setUTCDate(Math.min(d, lastDay));
  return target.toISOString().slice(0, 10);
}
