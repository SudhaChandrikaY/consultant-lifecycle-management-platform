import { client } from './client';
import type {
  BenchReadyReport,
  ConsultantPipelineReport,
  PlacementsByRecruiterReport,
  SubmissionsByRecruiterReport,
  VendorClientActivityReport,
} from './types';

const BASE = '/api/reports';
type Range = { from?: string; to?: string };

export const reportsApi = {
  submissionsByRecruiter: (range: Range) => client.get<SubmissionsByRecruiterReport>(`${BASE}/submissions-by-recruiter`, range),
  placementsByRecruiter: (range: Range) => client.get<PlacementsByRecruiterReport>(`${BASE}/placements-by-recruiter`, range),
  consultantPipeline: () => client.get<ConsultantPipelineReport>(`${BASE}/consultant-pipeline`),
  benchReady: () => client.get<BenchReadyReport>(`${BASE}/bench-ready`),
  vendorClientActivity: (range: Range) => client.get<VendorClientActivityReport>(`${BASE}/vendor-client-activity`, range),
};
