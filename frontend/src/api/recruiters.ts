import { client, type Query } from './client';
import type { LinkableUser, PageResponse, RecruiterDetail, RecruiterListItem, RecruiterRequest, RecruiterStatus } from './types';

const BASE = '/api/recruiters';

export const recruitersApi = {
  list: (query: Query) => client.get<PageResponse<RecruiterListItem>>(BASE, query),
  /** Active recruiters for assignment pickers and filters (at most 100). */
  active: () => client.get<PageResponse<RecruiterListItem>>(BASE, { status: 'ACTIVE', size: 100, sort: 'fullName,asc' }),
  all: () => client.get<PageResponse<RecruiterListItem>>(BASE, { size: 100, sort: 'fullName,asc' }),
  get: (id: number | string) => client.get<RecruiterDetail>(`${BASE}/${id}`),
  create: (body: RecruiterRequest) => client.post<RecruiterDetail>(BASE, body),
  update: (id: number | string, body: RecruiterRequest) => client.put<RecruiterDetail>(`${BASE}/${id}`, body),
  changeStatus: (id: number | string, body: { status: RecruiterStatus; confirm?: boolean; version: number }) =>
    client.post<RecruiterDetail>(`${BASE}/${id}/status`, body),
  linkableUsers: () => client.get<LinkableUser[]>(`${BASE}/linkable-users`),
};
