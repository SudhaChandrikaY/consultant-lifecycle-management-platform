import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router';
import { fieldErrorsOf } from '../../api/errors';
import { placementsApi } from '../../api/placements';
import { useAuth } from '../../auth/AuthProvider';
import { can } from '../../auth/permissions';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import HistoryList from '../../components/HistoryList';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';
import { formatDate, formatDateTime, formatMoney } from '../../labels';

export default function PlacementDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const detail = useApi(() => placementsApi.get(id!), [id]);
  const [form, setForm] = useState({ startDate: '', billRate: '', term: '' });
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);

  const p = detail.data;
  useEffect(() => {
    if (p) setForm({ startDate: p.startDate, billRate: String(p.billRate), term: String(p.contractTermMonths) });
  }, [p]);

  const save = async (e: FormEvent) => {
    e.preventDefault();
    if (!p) return;
    setError(null);
    setSaved(false);
    try {
      detail.setData(
        await placementsApi.edit(p.id, {
          startDate: form.startDate,
          billRate: Number(form.billRate),
          contractTermMonths: Number(form.term),
          version: p.version,
        }),
      );
      setSaved(true);
    } catch (err) {
      setError(err);
    }
  };

  if (detail.loading && !p) return <PageLayout title="Placement"><LoadingState /></PageLayout>;
  if (detail.error) return <PageLayout title="Placement"><ErrorBanner error={detail.error} /></PageLayout>;
  if (!p) return null;

  const banner = error && fieldErrorsOf(error).length === 0 ? error : null;

  return (
    <PageLayout title={`Placement — ${p.consultant.fullName}`}>
      <div className="grid-2">
        <section className="card">
          <h2>Details</h2>
          <dl className="field-list">
            <dt>Consultant</dt>
            <dd>
              <Link to={`/consultants/${p.consultant.id}`}>{p.consultant.fullName}</Link>
            </dd>
            <dt>Recruiter</dt>
            <dd>{p.recruiter.fullName}</dd>
            <dt>Client</dt>
            <dd>{p.client.name}</dd>
            <dt>Vendor</dt>
            <dd>{p.vendor.name}</dd>
            <dt>Job title</dt>
            <dd>{p.jobTitle}</dd>
            <dt>Start date</dt>
            <dd>{formatDate(p.startDate)}</dd>
            <dt>Bill rate</dt>
            <dd>{formatMoney(p.billRate)}</dd>
            <dt>Contract term</dt>
            <dd>{p.contractTermMonths} months</dd>
            <dt>Expected end</dt>
            <dd>{formatDate(p.expectedEndDate)}</dd>
            <dt>Source submission</dt>
            <dd>
              <Link to={`/submissions/${p.submissionId}`}>#{p.submissionId}</Link>
            </dd>
            <dt>Created</dt>
            <dd>
              {formatDateTime(p.createdAt)}
              {p.createdBy ? ` by ${p.createdBy}` : ''}
            </dd>
          </dl>
        </section>
        {can.editPlacement(user?.role) && (
          <section className="card">
            <h2>Edit placement</h2>
            {saved && (
              <div className="banner banner--info" role="status">
                Saved.
              </div>
            )}
            <ErrorBanner error={banner} onReload={() => { setError(null); detail.reload(); }} />
            <form className="form" onSubmit={save} noValidate>
              <FormField label="Start date" name="startDate" error={error} required>
                {(f) => <input {...f} type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />}
              </FormField>
              <FormField label="Bill rate (USD/hr)" name="billRate" error={error} required>
                {(f) => <input {...f} type="number" min={0.01} step={0.01} value={form.billRate} onChange={(e) => setForm({ ...form, billRate: e.target.value })} />}
              </FormField>
              <FormField label="Contract term (months)" name="contractTermMonths" error={error} required>
                {(f) => <input {...f} type="number" min={1} max={60} value={form.term} onChange={(e) => setForm({ ...form, term: e.target.value })} />}
              </FormField>
              <div className="form-actions">
                <button type="submit" className="primary">
                  Save changes
                </button>
              </div>
            </form>
          </section>
        )}
      </div>
      <section className="card">
        <h2>History</h2>
        <HistoryList entries={p.history} />
      </section>
    </PageLayout>
  );
}
