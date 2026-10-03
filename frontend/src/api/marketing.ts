import { client, type Query } from './client';
import type { HistoryEntry, MarketingDetail, MarketingListItem, MarketingStatus, Note, PageResponse } from './types';

const BASE = '/api/marketing-assignments';

export const marketingApi = {
  list: (query: Query) => client.get<PageResponse<MarketingListItem>>(BASE, query),
  get: (id: number | string) => client.get<MarketingDetail>(`${BASE}/${id}`),
  create: (body: { consultantId: number; ownerRecruiterId?: number | null; startDate: string; targetDate: string }) =>
    client.post<MarketingDetail>(BASE, body),
  updateDates: (id: number | string, body: { startDate: string; targetDate: string; version: number }) =>
    client.put<MarketingDetail>(`${BASE}/${id}`, body),
  transition: (id: number | string, body: { targetStatus: MarketingStatus; reason?: string; version: number }) =>
    client.post<MarketingDetail>(`${BASE}/${id}/transition`, body),
  addNote: (id: number | string, body: string) => client.post<Note>(`${BASE}/${id}/notes`, { body }),
  history: (id: number | string) => client.get<PageResponse<HistoryEntry>>(`${BASE}/${id}/history`, { size: 50 }),
};
