import { useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { fieldErrorsOf } from '../../api/errors';
import { expectedEndDate, placementsApi } from '../../api/placements';
import { submissionsApi } from '../../api/submissions';
import type { OtherOpenSubmission, PlacementCreated } from '../../api/types';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import StatusBadge from '../../components/StatusBadge';
import { useApi } from '../../hooks/useApi';
import { formatDate, SUBMISSION_STATUS_LABELS } from '../../labels';

/** /placements/new?submissionId= — pre-filled from the Offer submission (AS 6.1). */
export default function PlacementFormPage() {
  const [params] = useSearchParams();
  const submissionId = params.get('submissionId');
  const draft = useApi(
    () => (submissionId ? placementsApi.draft(submissionId) : Promise.resolve(null)),
    [submissionId],
  );
  const [startDate, setStartDate] = useState('');
  const [billRate, setBillRate] = useState('');
  const [term, setTerm] = useState('12');
  const [error, setError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);
  const [created, setCreated] = useState<PlacementCreated | null>(null);

  useEffect(() => {
    if (draft.data) setBillRate(String(draft.data.billRate));
  }, [draft.data]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!draft.data) return;
    setSaving(true);
    setError(null);
    try {
      setCreated(
        await placementsApi.create({
          submissionId: draft.data.submissionId,
          startDate,
          billRate: billRate.trim() === '' ? null : Number(billRate),
          contractTermMonths: term.trim() === '' ? null : Number(term),
        }),
      );
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  if (created) return <PlacementCreatedView created={created} />;
  if (!submissionId) {
    return (
      <PageLayout title="New placement">
        <div className="card">Open a submission at Offer and use “Create Placement”.</div>
      </PageLayout>
    );
  }
  if (draft.loading) return <PageLayout title="New placement"><LoadingState /></PageLayout>;
  if (draft.error || !draft.data) return <PageLayout title="New placement"><ErrorBanner error={draft.error} /></PageLayout>;

  const d = draft.data;
  const end = expectedEndDate(startDate, Number(term));
  const banner = error && fieldErrorsOf(error).length === 0 ? error : null;

  return (
    <PageLayout title={`New placement — ${d.consultant.fullName}`}>
      <div className="card">
        <dl className="field-list" style={{ marginBottom: 16 }}>
          <dt>Consultant</dt>
          <dd>{d.consultant.fullName}</dd>
          <dt>Recruiter</dt>
          <dd>{d.recruiter.fullName}</dd>
          <dt>Vendor</dt>
          <dd>{d.vendor.name}</dd>
          <dt>Client</dt>
          <dd>{d.client.name}</dd>
          <dt>Job title</dt>
          <dd>{d.jobTitle}</dd>
          <dt>Submitted</dt>
          <dd>{formatDate(d.submittedDate)}</dd>
        </dl>
        <ErrorBanner error={banner} />
        <form className="form" onSubmit={submit} noValidate>
          <FormField label="Start date" name="startDate" error={error} required hint="On or after the submitted date">
            {(p) => (
              <input {...p} type="date" value={startDate} min={d.submittedDate ?? undefined} onChange={(e) => setStartDate(e.target.value)} />
            )}
          </FormField>
          <FormField label="Bill rate (USD/hr)" name="billRate" error={error} required>
            {(p) => <input {...p} type="number" min={0.01} step={0.01} value={billRate} onChange={(e) => setBillRate(e.target.value)} />}
          </FormField>
          <FormField label="Contract term (months)" name="contractTermMonths" error={error} required hint="Whole months, 1–60">
            {(p) => <input {...p} type="number" min={1} max={60} step={1} value={term} onChange={(e) => setTerm(e.target.value)} />}
          </FormField>
          <p className="muted">Expected end date: {end ? formatDate(end) : '—'}</p>
          <div className="form-actions">
            <button type="submit" className="primary" disabled={saving || !startDate}>
              {saving ? 'Saving…' : 'Create placement'}
            </button>
            <Link className="button" to={`/submissions/${d.submissionId}`}>
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </PageLayout>
  );
}

/** After a 201: the consultant's other open submissions, each with a Withdraw action (FR-076). */
function PlacementCreatedView({ created }: { created: PlacementCreated }) {
  const [rows, setRows] = useState<OtherOpenSubmission[]>(created.otherOpenSubmissions);
  const [withdrawn, setWithdrawn] = useState<number[]>([]);
  const [error, setError] = useState<unknown>(null);

  const withdraw = async (row: OtherOpenSubmission) => {
    setError(null);
    try {
      const updated = await submissionsApi.changeStatus(row.id, { targetStatus: 'WITHDRAWN', version: row.version });
      setRows((rs) => rs.map((r) => (r.id === row.id ? { ...r, status: updated.status, version: updated.version } : r)));
      setWithdrawn((w) => [...w, row.id]);
    } catch (err) {
      setError(err);
    }
  };

  return (
    <PageLayout title="Placement created">
      <div className="banner banner--info" role="status">
        {created.consultant.fullName} is placed at {created.client.name} starting {formatDate(created.startDate)}.{' '}
        <Link to={`/placements/${created.id}`}>View placement</Link>
      </div>
      <section className="card" aria-labelledby="other-open-heading">
        <h2 id="other-open-heading">Other open submissions</h2>
        <ErrorBanner error={error} />
        {rows.length === 0 ? (
          <p className="muted">The consultant has no other open submissions.</p>
        ) : (
          <>
            <p className="muted">Nothing was changed automatically. Withdraw any that should no longer proceed.</p>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th scope="col">Job title</th>
                    <th scope="col">Vendor</th>
                    <th scope="col">Client</th>
                    <th scope="col">Status</th>
                    <th scope="col" />
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <Link to={`/submissions/${r.id}`}>{r.jobTitle}</Link>
                      </td>
                      <td>{r.vendorName}</td>
                      <td>{r.clientName}</td>
                      <td>
                        <StatusBadge status={r.status} label={SUBMISSION_STATUS_LABELS[r.status]} />
                      </td>
                      <td>
                        {withdrawn.includes(r.id) ? (
                          <span className="muted">Withdrawn</span>
                        ) : (
                          <button type="button" onClick={() => void withdraw(r)}>
                            Withdraw
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>
    </PageLayout>
  );
}
