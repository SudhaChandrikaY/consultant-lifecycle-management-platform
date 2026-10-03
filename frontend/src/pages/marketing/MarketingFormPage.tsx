import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { consultantsApi } from '../../api/consultants';
import { fieldErrorsOf, problemOf } from '../../api/errors';
import { marketingApi } from '../../api/marketing';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** /marketing/new?consultantId= — start a Draft marketing assignment for a Ready consultant. */
export default function MarketingFormPage() {
  const [params] = useSearchParams();
  const consultantId = params.get('consultantId');
  const navigate = useNavigate();
  const consultant = useApi(
    () => (consultantId ? consultantsApi.get(consultantId) : Promise.resolve(null)),
    [consultantId],
  );
  const [startDate, setStartDate] = useState(todayIso());
  const [targetDate, setTargetDate] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!consultantId) return;
    setSaving(true);
    setError(null);
    try {
      const created = await marketingApi.create({ consultantId: Number(consultantId), startDate, targetDate });
      navigate(`/marketing/${created.id}`);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  if (!consultantId) {
    return (
      <PageLayout title="Start marketing">
        <div className="card">
          Choose a Ready consultant from the <Link to="/consultants?status=READY">consultant list</Link> and use “Start
          marketing”.
        </div>
      </PageLayout>
    );
  }
  if (consultant.loading) return <PageLayout title="Start marketing"><LoadingState /></PageLayout>;
  if (consultant.error) return <PageLayout title="Start marketing"><ErrorBanner error={consultant.error} /></PageLayout>;

  const problem = problemOf(error);
  const existingId = problem?.code === 'OPEN_ASSIGNMENT_EXISTS' ? Number(problem.existingRecordId) : null;
  const banner =
    problem?.code === 'OPEN_ASSIGNMENT_EXISTS' || problem?.code === 'CONSULTANT_NOT_ELIGIBLE' || fieldErrorsOf(error).length > 0
      ? null
      : error;

  return (
    <PageLayout title={`Start marketing — ${consultant.data?.firstName ?? ''} ${consultant.data?.lastName ?? ''}`}>
      <div className="card">
        {existingId !== null && (
          <div className="banner banner--warning" role="alert">
            This consultant already has an open marketing assignment.{' '}
            <Link to={`/marketing/${existingId}`}>Open the existing assignment</Link>
          </div>
        )}
        {problem?.code === 'CONSULTANT_NOT_ELIGIBLE' && (
          <div className="banner banner--error" role="alert">
            Consultant must be Ready to start marketing.
          </div>
        )}
        <ErrorBanner error={banner} />
        <form className="form" onSubmit={submit} noValidate>
          <FormField label="Start date" name="startDate" error={error} required>
            {(p) => <input {...p} type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />}
          </FormField>
          <FormField label="Target date" name="targetDate" error={error} required hint="On or after the start date">
            {(p) => <input {...p} type="date" value={targetDate} min={startDate} onChange={(e) => setTargetDate(e.target.value)} />}
          </FormField>
          <p className="muted small">
            Owner: {consultant.data?.assignedRecruiter?.fullName ?? 'the consultant’s assigned recruiter'} (and their
            team). The assignment starts as Draft.
          </p>
          <div className="form-actions">
            <button type="submit" className="primary" disabled={saving || !startDate || !targetDate}>
              {saving ? 'Saving…' : 'Create draft'}
            </button>
            <Link className="button" to={`/consultants/${consultantId}`}>
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </PageLayout>
  );
}
