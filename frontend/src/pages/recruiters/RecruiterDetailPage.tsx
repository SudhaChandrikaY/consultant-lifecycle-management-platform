import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { problemOf } from '../../api/errors';
import { recruitersApi } from '../../api/recruiters';
import type { RecruiterStatus } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import ConfirmDialog from '../../components/ConfirmDialog';
import ErrorBanner from '../../components/ErrorBanner';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import StatusBadge from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';

export default function RecruiterDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const detail = useApi(() => recruitersApi.get(id!), [id]);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [confirmCount, setConfirmCount] = useState<number | null>(null);
  const [flagged, setFlagged] = useState<number | null>(null);

  const recruiter = detail.data;

  const changeStatus = async (status: RecruiterStatus, confirm?: boolean) => {
    if (!recruiter) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await recruitersApi.changeStatus(recruiter.id, { status, confirm, version: recruiter.version });
      detail.setData(updated);
      setConfirmCount(null);
      setFlagged(updated.consultantsFlaggedForReassignment ?? null);
    } catch (err) {
      const problem = problemOf(err);
      if (problem?.code === 'CONFIRMATION_REQUIRED') {
        setConfirmCount(Number(problem.affectedConsultantCount ?? 0));
      } else {
        setError(err);
      }
    } finally {
      setBusy(false);
    }
  };

  if (detail.loading && !recruiter) return <PageLayout title="Recruiter"><LoadingState /></PageLayout>;
  if (detail.error) return <PageLayout title="Recruiter"><ErrorBanner error={detail.error} /></PageLayout>;
  if (!recruiter) return null;

  const admin = can.manageRecruiters(user?.role);

  return (
    <PageLayout
      title={
        <>
          {recruiter.fullName} <StatusBadge status={recruiter.status} />
        </>
      }
      actions={
        admin && (
          <>
            {recruiter.status === 'ACTIVE' ? (
              <button type="button" className="danger" disabled={busy} onClick={() => void changeStatus('INACTIVE')}>
                Deactivate
              </button>
            ) : (
              <button type="button" disabled={busy} onClick={() => void changeStatus('ACTIVE')}>
                Activate
              </button>
            )}
            <Link className="button" to={`/recruiters/${recruiter.id}/edit`}>
              Edit
            </Link>
          </>
        )
      }
    >
      <ErrorBanner
        error={error}
        onReload={() => {
          setError(null);
          detail.reload();
        }}
      />
      {flagged !== null && flagged > 0 && (
        <div className="banner banner--info" role="status">
          {flagged} consultant{flagged === 1 ? ' is' : 's are'} now flagged for reassignment.{' '}
          <Link to="/consultants?needsReassignment=true">View them</Link>
        </div>
      )}
      <section className="card">
        <dl className="field-list">
          <dt>Email</dt>
          <dd>{recruiter.email}</dd>
          <dt>Phone</dt>
          <dd>{recruiter.phone ?? '—'}</dd>
          <dt>Team</dt>
          <dd>{recruiter.team.name}</dd>
          <dt>Region</dt>
          <dd>{recruiter.region.name}</dd>
          <dt>Assigned consultants</dt>
          <dd>
            <Link to={`/consultants?recruiterId=${recruiter.id}`}>{recruiter.assignedConsultantCount}</Link>
          </dd>
          <dt>Sign-in account</dt>
          <dd>{recruiter.linkedUser ? `${recruiter.linkedUser.displayName} (${recruiter.linkedUser.username})` : 'Not linked'}</dd>
        </dl>
      </section>
      {confirmCount !== null && (
        <ConfirmDialog
          title="Deactivate recruiter?"
          confirmLabel="Deactivate"
          danger
          busy={busy}
          onConfirm={() => void changeStatus('INACTIVE', true)}
          onCancel={() => setConfirmCount(null)}
        >
          <p>
            {confirmCount} consultant{confirmCount === 1 ? ' is' : 's are'} still assigned and will be flagged for
            reassignment.
          </p>
        </ConfirmDialog>
      )}
    </PageLayout>
  );
}
