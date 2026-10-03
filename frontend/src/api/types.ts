// Hand-written from specs/001-clmp-mvp/contracts/rest-api.md. Each story appends what it consumes.

export type Role = 'ADMIN' | 'MANAGER' | 'RECRUITER' | 'HR_OPERATIONS';

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  role: Role;
  recruiterId: number | null;
  sessionTimeoutMinutes: number;
}

export interface FieldErrorItem {
  field: string;
  message: string;
}

export interface ApiProblem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code: string;
  fieldErrors?: FieldErrorItem[];
  [extra: string]: unknown;
}
