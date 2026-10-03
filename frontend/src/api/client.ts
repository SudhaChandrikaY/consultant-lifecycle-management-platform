import type { ApiProblem } from './types';

export const SESSION_EXPIRED_EVENT = 'clmp:session-expired';

/** Thrown for every non-2xx response; carries the server's ProblemDetail. */
export class ApiError extends Error {
  readonly problem: ApiProblem;

  constructor(problem: ApiProblem) {
    super(problem.detail ?? problem.title ?? `Request failed (${problem.status})`);
    this.name = 'ApiError';
    this.problem = problem;
  }

  get status(): number {
    return this.problem.status;
  }

  get code(): string {
    return this.problem.code;
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH';
type Query = Record<string, string | number | boolean | null | undefined | Array<string | number>>;

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.substring(name.length + 1)) : null;
}

async function ensureCsrfToken(): Promise<string | null> {
  let token = readCookie('XSRF-TOKEN');
  if (!token) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' });
    token = readCookie('XSRF-TOKEN');
  }
  return token;
}

/** Serializes a query object; arrays become repeated keys (status=A&status=B). */
export function toQueryString(query?: Query): string {
  if (!query) return '';
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue;
    if (Array.isArray(value)) {
      value.forEach((v) => params.append(key, String(v)));
    } else {
      params.append(key, String(value));
    }
  }
  const s = params.toString();
  return s ? `?${s}` : '';
}

async function request<T>(method: Method, path: string, body?: unknown, query?: Query): Promise<T> {
  const url = `${path}${toQueryString(query)}`;
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (method !== 'GET') {
    const token = await ensureCsrfToken();
    if (token) headers['X-XSRF-TOKEN'] = token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
  }

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers,
      credentials: 'same-origin',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError({ status: 0, code: 'NETWORK_ERROR', detail: 'The server could not be reached.' });
  }

  if (response.status === 401 && path !== '/api/auth/login') {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  }

  if (!response.ok) {
    throw new ApiError(await parseProblem(response));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function parseProblem(response: Response): Promise<ApiProblem> {
  const contentType = response.headers.get('Content-Type') ?? '';
  if (contentType.includes('json')) {
    try {
      const problem = (await response.json()) as ApiProblem;
      return { ...problem, status: problem.status ?? response.status, code: problem.code ?? `HTTP_${response.status}` };
    } catch {
      // fall through to the generic problem below
    }
  }
  return { status: response.status, code: `HTTP_${response.status}`, detail: response.statusText };
}

export const client = {
  get: <T>(path: string, query?: Query) => request<T>('GET', path, undefined, query),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body: unknown) => request<T>('PATCH', path, body),
};

export type { Query };
