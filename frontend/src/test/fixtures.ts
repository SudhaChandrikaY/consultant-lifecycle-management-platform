import type { ReferenceData } from '../api/types';

export const REFERENCE: ReferenceData = {
  teams: [
    { id: 1, code: 'JAVA', name: 'Java' },
    { id: 2, code: 'DATA', name: 'Data & Analytics' },
  ],
  regions: [
    { id: 1, code: 'EAST', name: 'East' },
    { id: 2, code: 'WEST', name: 'West' },
  ],
  visaTypes: ['US_CITIZEN', 'GREEN_CARD', 'H1B', 'H4_EAD', 'L2_EAD', 'OPT', 'STEM_OPT', 'CPT', 'TN', 'OTHER'],
  consultantStatuses: ['BENCH', 'READY', 'MARKETING', 'INTERVIEWING', 'PLACED', 'ACTIVE_PROJECT', 'HOLD', 'INACTIVE'],
};
