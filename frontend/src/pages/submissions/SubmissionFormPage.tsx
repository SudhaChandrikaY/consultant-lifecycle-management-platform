import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { consultantsApi } from '../../api/consultants';
import { fieldErrorsOf, problemOf } from '../../api/errors';
import { submissionsApi } from '../../api/submissions';
import type { DuplicateSummary, SubmissionCreateRequest } from '../../api/types';
import ConfirmDialog from '../../components/ConfirmDialog';
import CounterpartyPicker, { type CounterpartyValue } from '../../components/CounterpartyPicker';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';
import { formatDate, SUBMISSION_STATUS_LABELS } from '../../labels';

const EMPTY_PARTY: CounterpartyValue = { id: null, name: '' };

/** /submissions/new?consultantId= — create a submission, with the FR-055 duplicate confirmation. */
export default function SubmissionFormPage() {
  const [params] = useSearchParams();
  const consultantId = params.get('consultantId');
  const navigate = useNavigate();
  const consultant = useApi(
    () => (consultantId ? consultantsApi.get(consultantId) : Promise.resolve(null)),
    [consultantId],
  );
  const [vendor, setVendor] = useState<CounterpartyValue>(EMPTY_PARTY);
  const [clientParty, setClientParty] = useState<CounterpartyValue>(EMPTY_PARTY);
  const [jobTitle, setJobTitle] = useState('');
  const [billRate, setBillRate] = useState('');
  const [submitNow, setSubmitNow] = useState(true);
  const [submittedDate, setSubmittedDate] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);
  const [pending, setPending] = useState<{ body: SubmissionCreateRequest; duplicates: DuplicateSummary[] } | null>(null);

  const send = async (body: SubmissionCreateRequest) => {
    setSaving(true);
    setError(null);
    try {
      const created = await submissionsApi.create(body);
      setPending(null);
      navigate(`/submissions/${created.id}`);
    } catch (err) {
      const problem = problemOf(err);
      if (problem?.code === 'DUPLICATE_SUBMISSION' && !body.acknowledgeDuplicate) {
        setPending({ body, duplicates: (problem.duplicates as DuplicateSummary[]) ?? [] });
      } else {
        setPending(null);
        setError(err);
      }
    } finally {
      setSaving(false);
    }
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!consultantId) return;
    const body: SubmissionCreateRequest = {
      consultantId: Number(consultantId),
      vendorId: vendor.id,
      vendorName: vendor.id === null ? vendor.name.trim() || null : null,
      clientId: clientParty.id,
      clientName: clientParty.id === null ? clientParty.name.trim() || null : null,
      jobTitle: jobTitle.trim(),
      billRate: billRate.trim() === '' ? null : Number(billRate),
      submitNow,
      submittedDate: submitNow && submittedDate ? submittedDate : null,
      note: note.trim() || null,
      acknowledgeDuplicate: false,
    };
    void send(body);
  };

  if (!consultantId) {
    return (
      <PageLayout title="New submission">
        <div className="card">
          Open a consultant and use “New submission”, or browse the <Link to="/consultants">consultant list</Link>.
        </div>
      </PageLayout>
    );
  }
  if (consultant.loading) return <PageLayout title="New submission"><LoadingState /></PageLayout>;
  if (consultant.error) return <PageLayout title="New submission"><ErrorBanner error={consultant.error} /></PageLayout>;

  const banner = error && fieldErrorsOf(error).length === 0 ? error : null;
  const c = consultant.data;

  return (
    <PageLayout title={`New submission — ${c?.firstName ?? ''} ${c?.lastName ?? ''}`}>
      <div className="card">
        <ErrorBanner error={banner} />
        <form className="form" onSubmit={onSubmit} noValidate>
          <FormField label="Vendor" name="vendorName" error={error} required hint="Pick an existing vendor or type a new name">
            {(p) => <CounterpartyPicker kind="vendors" value={vendor} onChange={setVendor} control={p} />}
          </FormField>
          <FormField label="Client" name="clientName" error={error} required hint="Pick an existing client or type a new name">
            {(p) => <CounterpartyPicker kind="clients" value={clientParty} onChange={setClientParty} control={p} />}
          </FormField>
          <FormField label="Job title" name="jobTitle" error={error} required>
            {(p) => <input {...p} value={jobTitle} maxLength={120} onChange={(e) => setJobTitle(e.target.value)} />}
          </FormField>
          <FormField label="Bill rate (USD/hr)" name="billRate" error={error} required>
            {(p) => <input {...p} type="number" min={0.01} step={0.01} value={billRate} onChange={(e) => setBillRate(e.target.value)} />}
          </FormField>
          <div className="field">
            <label htmlFor="field-submitNow">
              <input id="field-submitNow" type="checkbox" checked={submitNow} onChange={(e) => setSubmitNow(e.target.checked)} /> Submit now
              (otherwise saved as Draft)
            </label>
          </div>
          {submitNow && (
            <FormField label="Submitted date" name="submittedDate" error={error} hint="Defaults to today; cannot be in the future">
              {(p) => <input {...p} type="date" value={submittedDate} onChange={(e) => setSubmittedDate(e.target.value)} />}
            </FormField>
          )}
          <FormField label="Note" name="note" error={error}>
            {(p) => <textarea {...p} value={note} maxLength={2000} onChange={(e) => setNote(e.target.value)} />}
          </FormField>
          <div className="form-actions">
            <button type="submit" className="primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save submission'}
            </button>
            <Link className="button" to={`/consultants/${consultantId}`}>
              Cancel
            </Link>
          </div>
        </form>
      </div>
      {pending && (
        <ConfirmDialog
          title="Possible duplicate submission"
          confirmLabel="Submit anyway"
          busy={saving}
          onConfirm={() => void send({ ...pending.body, acknowledgeDuplicate: true })}
          onCancel={() => setPending(null)}
        >
          <p>This consultant was already submitted to the same vendor, client, and job title:</p>
          <ul>
            {pending.duplicates.map((d) => (
              <li key={d.id}>
                #{d.id} — {SUBMISSION_STATUS_LABELS[d.status]}, submitted {formatDate(d.submittedDate)} by {d.recruiterName}
              </li>
            ))}
          </ul>
          <p>Submit anyway? Your confirmation is recorded on the new submission.</p>
        </ConfirmDialog>
      )}
    </PageLayout>
  );
}
