// Display labels for API enum codes. Each story extends this file.
import type { ConsultantStatus, MarketingStatus, SubmissionStatus, VisaType } from './api/types';

const READINESS_ITEM_LABELS: Record<string, string> = {
  firstName: 'First name',
  lastName: 'Last name',
  email: 'Email',
  phone: 'Phone',
  primarySkill: 'Primary skill',
  yearsExperience: 'Years of experience',
  visaType: 'Visa type',
  assignedActiveRecruiter: 'An assigned active recruiter',
};

export function missingItemLabel(key: string): string {
  return READINESS_ITEM_LABELS[key] ?? key;
}

/** Fallback for any UPPER_SNAKE code without an explicit label. */
export function humanize(code: string): string {
  return code
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

export const CONSULTANT_STATUS_LABELS: Record<ConsultantStatus, string> = {
  BENCH: 'Bench',
  READY: 'Ready',
  MARKETING: 'Marketing',
  INTERVIEWING: 'Interviewing',
  PLACED: 'Placed',
  ACTIVE_PROJECT: 'Active Project',
  HOLD: 'Hold',
  INACTIVE: 'Inactive',
};

/** Verb labels for manual consultant status actions. */
export const CONSULTANT_ACTION_LABELS: Partial<Record<ConsultantStatus, string>> = {
  READY: 'Mark Ready',
  BENCH: 'Move to Bench',
  HOLD: 'Put on Hold',
  INACTIVE: 'Mark Inactive',
  ACTIVE_PROJECT: 'Mark Active Project',
};

export const VISA_TYPE_LABELS: Record<VisaType, string> = {
  US_CITIZEN: 'US Citizen',
  GREEN_CARD: 'Green Card',
  H1B: 'H-1B',
  H4_EAD: 'H-4 EAD',
  L2_EAD: 'L-2 EAD',
  OPT: 'OPT',
  STEM_OPT: 'STEM OPT',
  CPT: 'CPT',
  TN: 'TN',
  OTHER: 'Other',
};

export function label(map: Record<string, string>, code: string | null | undefined): string {
  if (!code) return '—';
  return map[code] ?? humanize(code);
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  const [y, m, d] = value.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  return new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

export const MARKETING_STATUS_LABELS: Record<MarketingStatus, string> = {
  DRAFT: 'Draft',
  ACTIVE: 'Active',
  HOLD: 'Hold',
  CLOSED: 'Closed',
};

export const SUBMISSION_STATUS_LABELS: Record<SubmissionStatus, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under Review',
  INTERVIEW_SCHEDULED: 'Interview Scheduled',
  INTERVIEW_CLEARED: 'Interview Cleared',
  REJECTED: 'Rejected',
  OFFER: 'Offer',
  PLACED: 'Placed',
  WITHDRAWN: 'Withdrawn',
};

export function formatMoney(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  return `$${Number(value).toFixed(2)}/hr`;
}
