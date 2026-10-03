import { client, type Query } from './client';
import type { Note, PageResponse, SubmissionCreateRequest, SubmissionDetail, SubmissionListItem, SubmissionStatus } from './types';

const BASE = '/api/submissions';

export const submissionsApi = {
  list: (query: Query) => client.get<PageResponse<SubmissionListItem>>(BASE, query),
  get: (id: number | string) => client.get<SubmissionDetail>(`${BASE}/${id}`),
  create: (body: SubmissionCreateRequest) => client.post<SubmissionDetail>(BASE, body),
  changeStatus: (
    id: number | string,
    body: { targetStatus: SubmissionStatus; note?: string | null; submittedDate?: string | null; version: number },
  ) => client.post<SubmissionDetail>(`${BASE}/${id}/status`, body),
  addNote: (id: number | string, body: string) => client.post<Note>(`${BASE}/${id}/notes`, { body }),
};
