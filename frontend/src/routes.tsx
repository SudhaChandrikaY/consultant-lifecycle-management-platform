import type { ReactNode } from 'react';
import { Route, Routes } from 'react-router';
import type { Role } from './api/types';
import { ROUTE_ROLES } from './auth/permissions';
import RequireRole from './auth/RequireRole';
import PageLayout from './components/PageLayout';
import DashboardPage from './pages/dashboard/DashboardPage';
import LoginPage from './pages/login/LoginPage';
import NotFound from './pages/NotFound';

/** Minimal placeholder for areas delivered by later stories. */
function Placeholder({ title }: { title: string }) {
  return (
    <PageLayout title={title}>
      <div className="card muted">This area is not available yet.</div>
    </PageLayout>
  );
}

interface AppRoute {
  path: string;
  roles: Role[];
  element: ReactNode;
}

// contracts/ui-routes.md
export const APP_ROUTES: AppRoute[] = [
  { path: '/', roles: ROUTE_ROLES.all, element: <DashboardPage /> },
  { path: '/recruiters', roles: ROUTE_ROLES.adminManager, element: <Placeholder title="Recruiters" /> },
  { path: '/recruiters/new', roles: ROUTE_ROLES.admin, element: <Placeholder title="New recruiter" /> },
  { path: '/recruiters/:id/edit', roles: ROUTE_ROLES.admin, element: <Placeholder title="Edit recruiter" /> },
  { path: '/recruiters/:id', roles: ROUTE_ROLES.adminManager, element: <Placeholder title="Recruiter" /> },
  { path: '/consultants', roles: ROUTE_ROLES.all, element: <Placeholder title="Consultants" /> },
  { path: '/consultants/new', roles: ROUTE_ROLES.consultantEditors, element: <Placeholder title="New consultant" /> },
  {
    path: '/consultants/:id/edit',
    roles: ROUTE_ROLES.consultantEditors,
    element: <Placeholder title="Edit consultant" />,
  },
  { path: '/consultants/:id', roles: ROUTE_ROLES.all, element: <Placeholder title="Consultant" /> },
  { path: '/marketing', roles: ROUTE_ROLES.commercial, element: <Placeholder title="Marketing" /> },
  { path: '/marketing/new', roles: ROUTE_ROLES.marketingCreators, element: <Placeholder title="New marketing" /> },
  { path: '/marketing/:id', roles: ROUTE_ROLES.commercial, element: <Placeholder title="Marketing assignment" /> },
  { path: '/submissions', roles: ROUTE_ROLES.commercial, element: <Placeholder title="Submissions" /> },
  {
    path: '/submissions/new',
    roles: ROUTE_ROLES.submissionCreators,
    element: <Placeholder title="New submission" />,
  },
  { path: '/submissions/:id', roles: ROUTE_ROLES.commercial, element: <Placeholder title="Submission" /> },
  { path: '/placements', roles: ROUTE_ROLES.commercial, element: <Placeholder title="Placements" /> },
  { path: '/placements/new', roles: ROUTE_ROLES.placementCreators, element: <Placeholder title="New placement" /> },
  { path: '/placements/:id', roles: ROUTE_ROLES.commercial, element: <Placeholder title="Placement" /> },
  { path: '/reports', roles: ROUTE_ROLES.adminManager, element: <Placeholder title="Reports" /> },
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
