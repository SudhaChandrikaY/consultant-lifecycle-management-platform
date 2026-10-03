import { client } from './client';
import type { IdName } from './types';

export type CounterpartyKind = 'vendors' | 'clients';

export const counterpartiesApi = {
  search: (kind: CounterpartyKind, q: string) => client.get<IdName[]>(`/api/${kind}`, { q }),
};
