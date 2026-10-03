import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { consultantsApi } from '../../api/consultants';
import type { ConsultantDetail, ConsultantStatus } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import ErrorBanner from '../../components/ErrorBanner';
import HistoryList from '../../components/HistoryList';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import ReasonDialog from '../../components/ReasonDialog';
import StatusBadge, { Badge } from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import AssignRecruiterDialog from './AssignRecruiterDialog';
import {
  CONSULTANT_ACTION_LABELS,
  CONSULTANT_STATUS_LABELS,
  formatDate,
  label,
  MARKETING_STATUS_LABELS,
  missingItemLabel,
  VISA_TYPE_LABELS,
} from '../../labels';

const NEEDS_REASON: ConsultantStatus[] = ['HOLD', 'INACTIVE'];

export default function ConsultantDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const detail = useApi(() => consultantsApi.get(id!), [id]);
  const history = useApi(() => consultantsApi.history(id!), [id]);
  const [actionError, setActionError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [reasonFor, setReasonFor] = useState<ConsultantStatus | null>(null);
  const [assigning, setAssigning] = useState(false);

  const consultant = detail.data;

  const changeStatus = async (target: ConsultantStatus, reason?: string) => {
    if (!consultant) return;
    setBusy(true);
    setActionError(null);
    try {
      const updated = await consultantsApi.changeStatus(consultant.id, {
        targetStatus: target,
        reason,
        version: consultant.version,
      });
      detail.setData(updated);
      setReasonFor(null);
      history.reload();
    } catch (err) {
      setActionError(err);
    } finally {
      setBusy(false);
    }
  };

  const reload = () => {
    setActionError(null);
    detail.reload();
    history.reload();
  };

  if (detail.loading && !consultant) return <PageLayout title="Consultant"><LoadingState /></PageLayout>;
  if (detail.error) {
    return (
      <PageLayout title="Consultant">
        <ErrorBanner error={detail.error} />
      </PageLayout>
    );
  }
  if (!consultant) return null;

  const commercial = can.seeCommercialDetails(user?.role);

  return (
    <PageLayout
      title={
        <>
          {consultant.firstName} {consultant.lastName} <StatusBadge status={consultant.status} />{' '}
          {consultant.needsReassignment && <Badge tone="danger">Needs reassignment</Badge>}
        </>
      }
      actions={
        <>
          {consultant.allowedStatusTransitions.map((target) => (
            <button
              key={target}
              type="button"
              disabled={busy}
              onClick={() => (NEEDS_REASON.includes(target) ? setReasonFor(target) : void changeStatus(target))}
            >
              {CONSULTANT_ACTION_LABELS[target] ?? `Set ${CONSULTANT_STATUS_LABELS[target]}`}
            </button>
          ))}
          {can.assignRecruiter(user?.role) && (
            <button type="button" onClick={() => setAssigning(true)}>
              {consultant.assignedRecruiter ? 'Reassign recruiter' : 'Assign recruiter'}
            </button>
          )}
          {can.editConsultant(user?.role) && (
            <Link className="button" to={`/consultants/${consultant.id}/edit`}>
              Edit
            </Link>
          )}
        </>
      }
    >
      {!reasonFor && <ErrorBanner error={actionError} onReload={reload} />}

      <div className="grid-2">
        <section className="card" aria-labelledby="profile-heading">
          <h2 id="profile-heading">Profile</h2>
          <dl className="field-list">
            <dt>Primary skill</dt>
            <dd>{consultant.primarySkill ?? '—'}</dd>
            <dt>Additional skills</dt>
            <dd>{consultant.additionalSkills ?? '—'}</dd>
            <dt>Years of experience</dt>
            <dd>{consultant.yearsExperience ?? '—'}</dd>
            <dt>Visa type</dt>
            <dd>{label(VISA_TYPE_LABELS, consultant.visaType)}</dd>
            <dt>Location</dt>
            <dd>{[consultant.city, consultant.state].filter(Boolean).join(', ') || '—'}</dd>
            <dt>Assigned recruiter</dt>
            <dd>
              <AssignedRecruiter consultant={consultant} />
            </dd>
          </dl>
          {consultant.missingReadinessItems.length > 0 &&
            (consultant.status === 'BENCH' || consultant.status === 'HOLD') && (
              <div className="banner banner--info" style={{ marginTop: 16, marginBottom: 0 }}>
                Needed before Ready:
                <ul>
                  {consultant.missingReadinessItems.map((item) => (
                    <li key={item}>{missingItemLabel(item)}</li>
                  ))}
                </ul>
              </div>
            )}
        </section>

        {consultant.contact && (
          <section className="card" aria-labelledby="contact-heading">
            <h2 id="contact-heading">Contact</h2>
            <dl className="field-list">
              <dt>Email</dt>
              <dd>{consultant.contact.email}</dd>
              <dt>Phone</dt>
              <dd>{consultant.contact.phone ?? '—'}</dd>
              <dt>Visa expiration</dt>
              <dd>{formatDate(consultant.contact.visaExpirationDate)}</dd>
              <dt>Notes</dt>
              <dd style={{ whiteSpace: 'pre-wrap' }}>{consultant.contact.notes ?? '—'}</dd>
            </dl>
          </section>
        )}
      </div>

      {commercial && <CommercialPanels consultant={consultant} />}

      <section className="card" aria-labelledby="history-heading">
        <h2 id="history-heading">History</h2>
        {history.error ? (
          <ErrorBanner error={history.error} />
        ) : history.data ? (
          <HistoryList entries={history.data.items} />
        ) : (
          <LoadingState />
        )}
      </section>

      {assigning && (
        <AssignRecruiterDialog
          consultant={consultant}
          onAssigned={(updated) => {
            detail.setData(updated);
            setAssigning(false);
            history.reload();
          }}
          onCancel={() => setAssigning(false)}
        />
      )}

      {reasonFor && (
        <ReasonDialog
          title={`${CONSULTANT_ACTION_LABELS[reasonFor] ?? CONSULTANT_STATUS_LABELS[reasonFor]}: reason`}
          confirmLabel={CONSULTANT_ACTION_LABELS[reasonFor]}
          error={actionError}
          busy={busy}
          onConfirm={(reason) => void changeStatus(reasonFor, reason)}
          onCancel={() => {
            setReasonFor(null);
            setActionError(null);
          }}
        />
      )}
    </PageLayout>
  );
}

function AssignedRecruiter({ consultant }: { consultant: ConsultantDetail }) {
  if (!consultant.assignedRecruiter) return <>Unassigned</>;
  return (
    <>
      {consultant.assignedRecruiter.fullName}
      {consultant.assignedRecruiter.status === 'INACTIVE' && (
        <span className="muted"> (inactive — needs reassignment)</span>
      )}
    </>
  );
}

/**
 * Marketing, Submissions, and Placements panels (US4–US6). Never rendered for HR_OPERATIONS;
 * the API also omits the data (FR-064).
 */
function CommercialPanels({ consultant }: { consultant: ConsultantDetail }) {
  return (
    <>
      <MarketingPanel consultant={consultant} />
    </>
  );
}

function MarketingPanel({ consultant }: { consultant: ConsultantDetail }) {
  const { user } = useAuth();
  const m = consultant.currentMarketingAssignment;
  return (
    <section className="card" aria-labelledby="marketing-heading">
      <h2 id="marketing-heading">Marketing</h2>
      {m ? (
        <dl className="field-list">
          <dt>Status</dt>
          <dd>
            <StatusBadge status={m.status} label={MARKETING_STATUS_LABELS[m.status]} />{' '}
            {m.overdue && <Badge tone="danger">Overdue</Badge>}
          </dd>
          <dt>Target date</dt>
          <dd>{formatDate(m.targetDate)}</dd>
          <dt />
          <dd>
            <Link to={`/marketing/${m.id}`}>View assignment</Link>
          </dd>
        </dl>
      ) : (
        <>
          <p className="muted">No open marketing assignment</p>
          {can.createMarketing(user?.role) && consultant.status === 'READY' && (
            <Link className="button primary" to={`/marketing/new?consultantId=${consultant.id}`}>
              Start marketing
            </Link>
          )}
        </>
      )}
    </section>
  );
}
