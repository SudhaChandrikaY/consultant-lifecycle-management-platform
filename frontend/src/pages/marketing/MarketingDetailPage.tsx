import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router';
import { marketingApi } from '../../api/marketing';
import type { MarketingStatus } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import HistoryList from '../../components/HistoryList';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import ReasonDialog from '../../components/ReasonDialog';
import StatusBadge, { Badge } from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import { formatDate, formatDateTime, MARKETING_STATUS_LABELS } from '../../labels';

function actionLabel(from: MarketingStatus, to: MarketingStatus): string {
  if (to === 'ACTIVE') return from === 'DRAFT' ? 'Activate' : 'Reopen';
  if (to === 'HOLD') return 'Put on Hold';
  return 'Close';
}

export default function MarketingDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const detail = useApi(() => marketingApi.get(id!), [id]);
  const history = useApi(() => marketingApi.history(id!), [id]);
  const [actionError, setActionError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [reasonFor, setReasonFor] = useState<MarketingStatus | null>(null);
  const [dates, setDates] = useState({ startDate: '', targetDate: '' });
  const [dateError, setDateError] = useState<unknown>(null);
  const [note, setNote] = useState('');
  const [noteError, setNoteError] = useState<unknown>(null);

  const m = detail.data;
  useEffect(() => {
    if (m) setDates({ startDate: m.startDate, targetDate: m.targetDate });
  }, [m]);

  const refresh = () => {
    detail.reload();
    history.reload();
  };

  const transition = async (target: MarketingStatus, reason?: string) => {
    if (!m) return;
    setBusy(true);
    setActionError(null);
    try {
      detail.setData(await marketingApi.transition(m.id, { targetStatus: target, reason, version: m.version }));
      setReasonFor(null);
      history.reload();
    } catch (err) {
      setActionError(err);
    } finally {
      setBusy(false);
    }
  };

  const saveDates = async (e: FormEvent) => {
    e.preventDefault();
    if (!m) return;
    setDateError(null);
    try {
      detail.setData(await marketingApi.updateDates(m.id, { ...dates, version: m.version }));
      history.reload();
    } catch (err) {
      setDateError(err);
    }
  };

  const addNote = async (e: FormEvent) => {
    e.preventDefault();
    if (!m || !note.trim()) return;
    setNoteError(null);
    try {
      await marketingApi.addNote(m.id, note.trim());
      setNote('');
      refresh();
    } catch (err) {
      setNoteError(err);
    }
  };

  if (detail.loading && !m) return <PageLayout title="Marketing assignment"><LoadingState /></PageLayout>;
  if (detail.error) return <PageLayout title="Marketing assignment"><ErrorBanner error={detail.error} /></PageLayout>;
  if (!m) return null;

  const editable = can.createMarketing(user?.role) && m.status !== 'CLOSED';
  const canNote = can.createMarketing(user?.role);
  const reasonRequired = reasonFor === 'HOLD' || (reasonFor === 'CLOSED' && m.status !== 'DRAFT');

  return (
    <PageLayout
      title={
        <>
          Marketing — {m.consultant.fullName}{' '}
          <StatusBadge status={m.status} label={MARKETING_STATUS_LABELS[m.status]} />{' '}
          {m.overdue && <Badge tone="danger">Overdue</Badge>}
        </>
      }
      actions={m.allowedTransitions.map((to) => (
        <button
          key={to}
          type="button"
          disabled={busy}
          onClick={() => (to === 'ACTIVE' ? void transition(to) : setReasonFor(to))}
        >
          {actionLabel(m.status, to)}
        </button>
      ))}
    >
      {!reasonFor && <ErrorBanner error={actionError} onReload={() => { setActionError(null); refresh(); }} />}
      <div className="grid-2">
        <section className="card">
          <h2>Details</h2>
          <dl className="field-list">
            <dt>Consultant</dt>
            <dd>
              <Link to={`/consultants/${m.consultant.id}`}>{m.consultant.fullName}</Link>
            </dd>
            <dt>Owner recruiter</dt>
            <dd>{m.ownerRecruiter.fullName}</dd>
            <dt>Team</dt>
            <dd>{m.team.name}</dd>
            <dt>Start date</dt>
            <dd>{formatDate(m.startDate)}</dd>
            <dt>Target date</dt>
            <dd>{formatDate(m.targetDate)}</dd>
            {m.holdReason && (
              <>
                <dt>Hold reason</dt>
                <dd>{m.holdReason}</dd>
              </>
            )}
            {m.closeReason && (
              <>
                <dt>Close reason</dt>
                <dd>{m.closeReason}</dd>
              </>
            )}
          </dl>
        </section>
        {editable && (
          <section className="card">
            <h2>Edit dates</h2>
            <ErrorBanner error={dateError && !(dateError as { problem?: { fieldErrors?: unknown[] } }).problem?.fieldErrors ? dateError : null} />
            <form className="form" onSubmit={saveDates} noValidate>
              <FormField label="Start date" name="startDate" error={dateError} required>
                {(p) => <input {...p} type="date" value={dates.startDate} onChange={(e) => setDates((d) => ({ ...d, startDate: e.target.value }))} />}
              </FormField>
              <FormField label="Target date" name="targetDate" error={dateError} required>
                {(p) => <input {...p} type="date" value={dates.targetDate} onChange={(e) => setDates((d) => ({ ...d, targetDate: e.target.value }))} />}
              </FormField>
              <div className="form-actions">
                <button type="submit">Save dates</button>
              </div>
            </form>
          </section>
        )}
      </div>

      <section className="card">
        <h2>Notes</h2>
        {m.notes.length === 0 ? (
          <p className="muted">No notes yet.</p>
        ) : (
          <ul className="history">
            {m.notes.map((n) => (
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
        <h2>History</h2>
        {history.data ? <HistoryList entries={history.data.items} /> : <LoadingState />}
      </section>

      {reasonFor && (
        <ReasonDialog
          title={`${actionLabel(m.status, reasonFor)}: reason`}
          confirmLabel={actionLabel(m.status, reasonFor)}
          required={reasonRequired}
          error={actionError}
          busy={busy}
          onConfirm={(reason) => void transition(reasonFor, reason || undefined)}
          onCancel={() => {
            setReasonFor(null);
            setActionError(null);
          }}
        />
      )}
    </PageLayout>
  );
}
