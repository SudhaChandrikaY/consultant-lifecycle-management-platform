// Mirrors specs/001-clmp-mvp/contracts/authorization-matrix.md. UX only: the backend enforces access.
import type { Role } from '../api/types';

export interface NavItem {
  label: string;
  path: string;
  roles: Role[];
}

const ALL: Role[] = ['ADMIN', 'MANAGER', 'RECRUITER', 'HR_OPERATIONS'];
const ADMIN_MANAGER: Role[] = ['ADMIN', 'MANAGER'];
const COMMERCIAL: Role[] = ['ADMIN', 'MANAGER', 'RECRUITER'];

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/', roles: ALL },
  { label: 'Recruiters', path: '/recruiters', roles: ADMIN_MANAGER },
  { label: 'Consultants', path: '/consultants', roles: ALL },
  { label: 'Marketing', path: '/marketing', roles: COMMERCIAL },
  { label: 'Submissions', path: '/submissions', roles: COMMERCIAL },
  { label: 'Placements', path: '/placements', roles: COMMERCIAL },
  { label: 'Reports', path: '/reports', roles: ADMIN_MANAGER },
];

export function navItemsFor(role: Role): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.includes(role));
}

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  MANAGER: 'Manager',
  RECRUITER: 'Recruiter',
  HR_OPERATIONS: 'HR Operations',
};

/** Route-level role sets, shared by routes.tsx and action checks. */
export const ROUTE_ROLES = {
  all: ALL,
  adminManager: ADMIN_MANAGER,
  commercial: COMMERCIAL,
  admin: ['ADMIN'] as Role[],
  consultantEditors: ['ADMIN', 'HR_OPERATIONS'] as Role[],
  marketingCreators: ['ADMIN', 'RECRUITER'] as Role[],
  submissionCreators: ['ADMIN', 'RECRUITER'] as Role[],
  placementCreators: ['ADMIN', 'RECRUITER'] as Role[],
};

const has = (role: Role | undefined, roles: Role[]) => role !== undefined && roles.includes(role);

export const can = {
  editConsultant: (role?: Role) => has(role, ['ADMIN', 'HR_OPERATIONS']),
  changeConsultantStatus: (role?: Role) => has(role, ['ADMIN', 'HR_OPERATIONS']),
  viewRecruiters: (role?: Role) => has(role, ADMIN_MANAGER),
  manageRecruiters: (role?: Role) => has(role, ['ADMIN']),
  assignRecruiter: (role?: Role) => has(role, ADMIN_MANAGER),
  createMarketing: (role?: Role) => has(role, ['ADMIN', 'RECRUITER']),
  createSubmission: (role?: Role) => has(role, ['ADMIN', 'RECRUITER']),
  updateSubmission: (role?: Role) => has(role, ['ADMIN', 'RECRUITER']),
  viewPlacements: (role?: Role) => has(role, COMMERCIAL),
  editPlacement: (role?: Role) => has(role, ['ADMIN']),
  viewReports: (role?: Role) => has(role, ADMIN_MANAGER),
  /** False for HR_OPERATIONS: no vendor, client, rate, marketing, submission, or placement data. */
  seeCommercialDetails: (role?: Role) => has(role, COMMERCIAL),
};
