import { client } from './client';
import type { Dashboard } from './types';

export const dashboardApi = {
  get: () => client.get<Dashboard>('/api/dashboard'),
};
