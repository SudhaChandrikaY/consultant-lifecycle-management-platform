import type { ReactNode } from 'react';
import { Route, Routes } from 'react-router';
import type { Role } from './api/types';
import { ROUTE_ROLES } from './auth/permissions';
import RequireRole from './auth/RequireRole';
import ConsultantDetailPage from './pages/consultants/ConsultantDetailPage';
import ConsultantFormPage from './pages/consultants/ConsultantFormPage';
import ConsultantListPage from './pages/consultants/ConsultantListPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import LoginPage from './pages/login/LoginPage';
import MarketingDetailPage from './pages/marketing/MarketingDetailPage';
import MarketingFormPage from './pages/marketing/MarketingFormPage';
import MarketingListPage from './pages/marketing/MarketingListPage';
import PlacementDetailPage from './pages/placements/PlacementDetailPage';
import PlacementFormPage from './pages/placements/PlacementFormPage';
import PlacementListPage from './pages/placements/PlacementListPage';
import ReportsPage from './pages/reports/ReportsPage';
import RecruiterDetailPage from './pages/recruiters/RecruiterDetailPage';
import RecruiterFormPage from './pages/recruiters/RecruiterFormPage';
import RecruiterListPage from './pages/recruiters/RecruiterListPage';
import SubmissionDetailPage from './pages/submissions/SubmissionDetailPage';
import SubmissionFormPage from './pages/submissions/SubmissionFormPage';
import SubmissionListPage from './pages/submissions/SubmissionListPage';
import NotFound from './pages/NotFound';

interface AppRoute {
  path: string;
  roles: Role[];
  element: ReactNode;
}

// contracts/ui-routes.md
export const APP_ROUTES: AppRoute[] = [
  { path: '/', roles: ROUTE_ROLES.all, element: <DashboardPage /> },
  { path: '/recruiters', roles: ROUTE_ROLES.adminManager, element: <RecruiterListPage /> },
  { path: '/recruiters/new', roles: ROUTE_ROLES.admin, element: <RecruiterFormPage /> },
  { path: '/recruiters/:id/edit', roles: ROUTE_ROLES.admin, element: <RecruiterFormPage /> },
  { path: '/recruiters/:id', roles: ROUTE_ROLES.adminManager, element: <RecruiterDetailPage /> },
  { path: '/consultants', roles: ROUTE_ROLES.all, element: <ConsultantListPage /> },
  { path: '/consultants/new', roles: ROUTE_ROLES.consultantEditors, element: <ConsultantFormPage /> },
  { path: '/consultants/:id/edit', roles: ROUTE_ROLES.consultantEditors, element: <ConsultantFormPage /> },
  { path: '/consultants/:id', roles: ROUTE_ROLES.all, element: <ConsultantDetailPage /> },
  { path: '/marketing', roles: ROUTE_ROLES.commercial, element: <MarketingListPage /> },
  { path: '/marketing/new', roles: ROUTE_ROLES.marketingCreators, element: <MarketingFormPage /> },
  { path: '/marketing/:id', roles: ROUTE_ROLES.commercial, element: <MarketingDetailPage /> },
  { path: '/submissions', roles: ROUTE_ROLES.commercial, element: <SubmissionListPage /> },
  { path: '/submissions/new', roles: ROUTE_ROLES.submissionCreators, element: <SubmissionFormPage /> },
  { path: '/submissions/:id', roles: ROUTE_ROLES.commercial, element: <SubmissionDetailPage /> },
  { path: '/placements', roles: ROUTE_ROLES.commercial, element: <PlacementListPage /> },
  { path: '/placements/new', roles: ROUTE_ROLES.placementCreators, element: <PlacementFormPage /> },
  { path: '/placements/:id', roles: ROUTE_ROLES.commercial, element: <PlacementDetailPage /> },
  { path: '/reports', roles: ROUTE_ROLES.adminManager, element: <ReportsPage /> },
];

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      {APP_ROUTES.map((r) => (
        <Route key={r.path} path={r.path} element={<RequireRole roles={r.roles}>{r.element}</RequireRole>} />
      ))}
      <Route
        path="*"
        element={
          <RequireRole roles={ROUTE_ROLES.all}>
            <NotFound />
          </RequireRole>
        }
      />
    </Routes>
  );
}
