import { CONSULTANT_STATUS_LABELS, humanize } from '../labels';

type Tone = 'neutral' | 'success' | 'info' | 'warning' | 'danger';

const TONES: Record<string, Tone> = {
  // consultant
  BENCH: 'neutral',
  READY: 'info',
  MARKETING: 'info',
  INTERVIEWING: 'warning',
  PLACED: 'success',
  ACTIVE_PROJECT: 'success',
  HOLD: 'warning',
  INACTIVE: 'danger',
  // recruiter
  ACTIVE: 'success',
  // marketing
  DRAFT: 'neutral',
  CLOSED: 'neutral',
  // submission
  SUBMITTED: 'info',
  UNDER_REVIEW: 'info',
  INTERVIEW_SCHEDULED: 'warning',
  INTERVIEW_CLEARED: 'warning',
  OFFER: 'success',
  REJECTED: 'danger',
  WITHDRAWN: 'neutral',
};

const LABELS: Record<string, string> = { ...CONSULTANT_STATUS_LABELS };

/** Colored status pill for any lifecycle status code. */
export default function StatusBadge({ status, label }: { status: string; label?: string }) {
  const tone = TONES[status] ?? 'neutral';
  return <span className={`badge badge--${tone}`}>{label ?? LABELS[status] ?? humanize(status)}</span>;
}

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: React.ReactNode }) {
  return <span className={`badge badge--${tone}`}>{children}</span>;
}
