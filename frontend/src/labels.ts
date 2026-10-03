// Display labels for API enum codes. Each story extends this file.

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
