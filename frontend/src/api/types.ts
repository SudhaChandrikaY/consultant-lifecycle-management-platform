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

// ---- US2: consultants, history, reference ----

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type ConsultantStatus =
  | 'BENCH'
  | 'READY'
  | 'MARKETING'
  | 'INTERVIEWING'
  | 'PLACED'
  | 'ACTIVE_PROJECT'
  | 'HOLD'
  | 'INACTIVE';

export type VisaType =
  | 'US_CITIZEN'
  | 'GREEN_CARD'
  | 'H1B'
  | 'H4_EAD'
  | 'L2_EAD'
  | 'OPT'
  | 'STEM_OPT'
  | 'CPT'
  | 'TN'
  | 'OTHER';

export type RecruiterStatus = 'ACTIVE' | 'INACTIVE';

export interface IdName {
  id: number;
  name: string;
}

export interface IdFullName {
  id: number;
  fullName: string;
}

export interface ReferenceItem {
  id: number;
  code: string;
  name: string;
}

export interface ReferenceData {
  teams: ReferenceItem[];
  regions: ReferenceItem[];
  visaTypes: VisaType[];
  consultantStatuses: ConsultantStatus[];
}

export type ChangeType =
  | 'CREATED'
  | 'STATUS'
  | 'RECRUITER_ASSIGNMENT'
  | 'OWNER_TRANSFER'
  | 'PROFILE_UPDATED'
  | 'FIELD_EDIT'
  | 'NOTE_ADDED';

export interface HistoryEntry {
  id: number;
  occurredAt: string;
  actor: string;
  changeType: ChangeType;
  field: string | null;
  oldValue: string | null;
  newValue: string | null;
  reason: string | null;
  note: string | null;
  description: string;
}

export interface ConsultantListItem {
  id: number;
  fullName: string;
  primarySkill: string | null;
  yearsExperience: number | null;
  visaType: VisaType | null;
  assignedRecruiter: IdFullName | null;
  status: ConsultantStatus;
  needsReassignment: boolean;
}

export interface ConsultantContact {
  email: string;
  phone: string | null;
  visaExpirationDate: string | null;
  notes: string | null;
}

export interface ConsultantDetail {
  id: number;
  firstName: string;
  lastName: string;
  city?: string;
  state?: string;
  primarySkill?: string;
  additionalSkills?: string;
  yearsExperience?: number;
  visaType?: VisaType;
  status: ConsultantStatus;
  needsReassignment: boolean;
  assignedRecruiter?: { id: number; fullName: string; status: RecruiterStatus };
  contact?: ConsultantContact;
  allowedStatusTransitions: ConsultantStatus[];
  missingReadinessItems: string[];
  version: number;
}

export interface ConsultantRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  city?: string | null;
  state?: string | null;
  primarySkill?: string | null;
  additionalSkills?: string | null;
  yearsExperience?: number | null;
  visaType?: VisaType | null;
  visaExpirationDate?: string | null;
  notes?: string | null;
  version?: number;
}

// ---- US3: recruiters ----

export interface RecruiterListItem {
  id: number;
  fullName: string;
  team: IdName;
  region: IdName;
  status: RecruiterStatus;
  assignedConsultantCount: number;
}

export interface LinkableUser {
  id: number;
  username: string;
  displayName: string;
}

export interface RecruiterDetail extends RecruiterListItem {
  email: string;
  phone?: string;
  linkedUser?: LinkableUser;
  version: number;
  consultantsFlaggedForReassignment?: number;
}

export interface RecruiterRequest {
  fullName: string;
  email: string;
  phone?: string | null;
  teamId: number | null;
  regionId: number | null;
  linkedUserId?: number | null;
  version?: number;
}
