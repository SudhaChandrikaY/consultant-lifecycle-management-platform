import { client } from './client';
import type { CurrentUser } from './types';

export const authApi = {
  csrf: () => client.get<void>('/api/auth/csrf'),
  login: (username: string, password: string) =>
    client.post<CurrentUser>('/api/auth/login', { username, password }),
  logout: () => client.post<void>('/api/auth/logout'),
  me: () => client.get<CurrentUser>('/api/auth/me'),
};
