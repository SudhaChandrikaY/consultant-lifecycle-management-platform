import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router';
import { submissionsApi } from '../../api/submissions';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import type { SubmissionStatus } from '../../api/types';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import HistoryList from '../../components/HistoryList';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import StatusBadge from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import { formatDate, formatDateTime, formatMoney, SUBMISSION_STATUS_LABELS } from '../../labels';

/** Read-only submission fields, timeline, append-only notes, and status changes (no general edit). */
export default function SubmissionDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const detail = useApi(() => submissionsApi.get(id!), [id]);
  const [target, setTarget] = useState<SubmissionStatus | ''>('');
  const [statusNote, setStatusNote] = useState('');
  const [submittedDate, setSubmittedDate] = useState('');
  const [statusError, setStatusError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [note, setNote] = useState('');
  const [noteError, setNoteError] = useState<unknown>(null);

  const s = detail.data;

  const changeStatus = async (e: FormEvent) => {
    e.preventDefault();
    if (!s || !target) return;
    setBusy(true);
    setStatusError(null);
    try {
      detail.setData(
        await submissionsApi.changeStatus(s.id, {
          targetStatus: target,
          note: statusNote.trim() || null,
          submittedDate: target === 'SUBMITTED' && submittedDate ? submittedDate : null,
          version: s.version,
        }),
      );
      setTarget('');
      setStatusNote('');
      setSubmittedDate('');
    } catch (err) {
      setStatusError(err);
    } finally {
      setBusy(false);
    }
  };

  const addNote = async (e: FormEvent) => {
    e.preventDefault();
    if (!s || !note.trim()) return;
    setNoteError(null);
    try {
      await submissionsApi.addNote(s.id, note.trim());
      setNote('');
      detail.reload();
    } catch (err) {
      setNoteError(err);
    }
  };

  if (detail.loading && !s) return <PageLayout title="Submission"><LoadingState /></PageLayout>;
  if (detail.error) return <PageLayout title="Submission"><ErrorBanner error={detail.error} /></PageLayout>;
  if (!s) return null;

  const canAct = s.allowedTransitions.length > 0;
  // MANAGER is read-only (FR-017). A RECRUITER who can open the page owns it (FR-063).
  const canNote = can.updateSubmission(user?.role);

  return (
    <PageLayout
      title={
        <>
          {s.jobTitle} — {s.consultant.fullName}{' '}
          <StatusBadge status={s.status} label={SUBMISSION_STATUS_LABELS[s.status]} />
        </>
      }
      actions={
        s.canCreatePlacement && (
          <Link className="button primary" to={`/placements/new?submissionId=${s.id}`}>
            Create Placement
          </Link>
        )
      }
    >
      <div className="grid-2">
        <section className="card">
          <h2>Details</h2>
          <dl className="field-list">
            <dt>Consultant</dt>
            <dd>
              <Link to={`/consultants/${s.consultant.id}`}>{s.consultant.fullName}</Link>
            </dd>
            <dt>Recruiter</dt>
            <dd>{s.recruiter.fullName}</dd>
            <dt>Vendor</dt>
            <dd>{s.vendor.name}</dd>
            <dt>Client</dt>
            <dd>{s.client.name}</dd>
            <dt>Job title</dt>
            <dd>{s.jobTitle}</dd>
            <dt>Submitted date</dt>
            <dd>{formatDate(s.submittedDate)}</dd>
            <dt>Bill rate</dt>
            <dd>{formatMoney(s.billRate)}</dd>
          </dl>
          {s.duplicateAcknowledgement && (
            <div className="banner banner--warning" style={{ marginTop: 16, marginBottom: 0 }}>
              Duplicate acknowledged by {s.duplicateAcknowledgement.acknowledgedBy} on{' '}
              {formatDateTime(s.duplicateAcknowledgement.acknowledgedAt)}. Earlier submissions:{' '}
              {s.duplicateAcknowledgement.earlierSubmissionIds.map((eid, i) => (
                <span key={eid}>
                  {i > 0 && ', '}
                  <Link to={`/submissions/${eid}`}>#{eid}</Link>
                </span>
              ))}
            </div>
          )}
        </section>
        {canAct && (
          <section className="card">
            <h2>Change status</h2>
            <ErrorBanner error={statusError} onReload={() => { setStatusError(null); detail.reload(); }} />
            <form className="form" onSubmit={changeStatus}>
              <FormField label="New status" name="targetStatus" error={statusError} required>
                {(p) => (
                  <select {...p} value={target} onChange={(e) => setTarget(e.target.value as SubmissionStatus)}>
                    <option value="">Choose…</option>
                    {s.allowedTransitions.map((t) => (
                      <option key={t} value={t}>
                        {SUBMISSION_STATUS_LABELS[t]}
                      </option>
                    ))}
                  </select>
                )}
              </FormField>
              {target === 'SUBMITTED' && (
                <FormField label="Submitted date" name="submittedDate" error={statusError} hint="Defaults to today">
                  {(p) => <input {...p} type="date" value={submittedDate} onChange={(e) => setSubmittedDate(e.target.value)} />}
                </FormField>
              )}
              <FormField label="Note (optional)" name="note" error={statusError}>
                {(p) => <textarea {...p} value={statusNote} maxLength={2000} onChange={(e) => setStatusNote(e.target.value)} />}
              </FormField>
              <div className="form-actions">
                <button type="submit" className="primary" disabled={busy || !target}>
                  Update status
                </button>
              </div>
            </form>
          </section>
        )}
      </div>

      <section className="card">
        <h2>Notes</h2>
        {s.notes.length === 0 ? (
          <p className="muted">No notes yet.</p>
        ) : (
          <ul className="history">
            {s.notes.map((n) => (
              <li key={n.id}>
                <div style={{ whiteSpace: 'pre-wrap' }}>{n.body}</div>
                <div className="history__meta">
                  {n.author} · {formatDateTime(n.createdAt)}
                </div>
              </li>
            ))}
          </ul>
        )}
        {canNote && (
          <form className="form" onSubmit={addNote} style={{ marginTop: 12 }}>
            <ErrorBanner error={noteError} />
            <FormField label="Add a note" name="body" error={noteError}>
              {(p) => <textarea {...p} value={note} maxLength={2000} onChange={(e) => setNote(e.target.value)} />}
            </FormField>
            <div className="form-actions">
              <button type="submit" disabled={!note.trim()}>
                Add note
              </button>
            </div>
          </form>
        )}
      </section>

      <section className="card">
        <h2>Timeline</h2>
        <HistoryList entries={s.timeline} />
      </section>
    </PageLayout>
  );
}
