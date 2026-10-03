import { client, type Query } from './client';
import type {
  ConsultantDetail,
  ConsultantListItem,
  ConsultantRequest,
  ConsultantStatus,
  HistoryEntry,
  PageResponse,
} from './types';

const BASE = '/api/consultants';

export const consultantsApi = {
  list: (query: Query) => client.get<PageResponse<ConsultantListItem>>(BASE, query),
  get: (id: number | string) => client.get<ConsultantDetail>(`${BASE}/${id}`),
  create: (body: ConsultantRequest) => client.post<ConsultantDetail>(BASE, body),
  update: (id: number | string, body: ConsultantRequest) => client.put<ConsultantDetail>(`${BASE}/${id}`, body),
  changeStatus: (id: number | string, body: { targetStatus: ConsultantStatus; reason?: string; version: number }) =>
    client.post<ConsultantDetail>(`${BASE}/${id}/status`, body),
  history: (id: number | string, page = 0, size = 50) =>
    client.get<PageResponse<HistoryEntry>>(`${BASE}/${id}/history`, { page, size }),
  skills: () => client.get<string[]>(`${BASE}/skills`),
};
