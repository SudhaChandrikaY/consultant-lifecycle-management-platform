import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { fieldErrorsOf } from '../../api/errors';
import { recruitersApi } from '../../api/recruiters';
import { referenceApi } from '../../api/reference';
import type { LinkableUser, RecruiterRequest } from '../../api/types';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';

interface FormState {
  fullName: string;
  email: string;
  phone: string;
  teamId: string;
  regionId: string;
  linkedUserId: string;
}

const EMPTY: FormState = { fullName: '', email: '', phone: '', teamId: '', regionId: '', linkedUserId: '' };

export default function RecruiterFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const reference = useApi(() => referenceApi.get(), []);
  const linkable = useApi(() => recruitersApi.linkableUsers(), []);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [currentLinked, setCurrentLinked] = useState<LinkableUser | null>(null);
  const [version, setVersion] = useState<number | undefined>();
  const [loading, setLoading] = useState(editing);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [error, setError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!id) return;
    recruitersApi
      .get(id)
      .then((r) => {
        setForm({
          fullName: r.fullName,
          email: r.email,
          phone: r.phone ?? '',
          teamId: String(r.team.id),
          regionId: String(r.region.id),
          linkedUserId: r.linkedUser ? String(r.linkedUser.id) : '',
        });
        setCurrentLinked(r.linkedUser ?? null);
        setVersion(r.version);
      })
      .catch(setLoadError)
      .finally(() => setLoading(false));
  }, [id]);

  const bind = (name: keyof FormState) => ({
    value: form[name],
    onChange: (e: { target: { value: string } }) => setForm((f) => ({ ...f, [name]: e.target.value })),
  });

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const body: RecruiterRequest = {
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      phone: form.phone.trim() || null,
      teamId: form.teamId ? Number(form.teamId) : null,
      regionId: form.regionId ? Number(form.regionId) : null,
      linkedUserId: form.linkedUserId ? Number(form.linkedUserId) : null,
      version,
    };
    try {
      const saved = editing ? await recruitersApi.update(id!, body) : await recruitersApi.create(body);
      navigate(`/recruiters/${saved.id}`);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  const title = editing ? 'Edit recruiter' : 'Add recruiter';
  if (loading) return <PageLayout title={title}><LoadingState /></PageLayout>;
  if (loadError) return <PageLayout title={title}><ErrorBanner error={loadError} /></PageLayout>;

  const users = [...(currentLinked ? [currentLinked] : []), ...(linkable.data ?? [])];
  const bannerError = error && fieldErrorsOf(error).length === 0 ? error : null;

  return (
    <PageLayout title={title}>
      <div className="card">
        <ErrorBanner error={bannerError} />
        <form className="form" onSubmit={onSubmit} noValidate>
          <FormField label="Full name" name="fullName" error={error} required>
            {(p) => <input {...p} {...bind('fullName')} maxLength={120} />}
          </FormField>
          <FormField label="Email" name="email" error={error} required>
            {(p) => <input {...p} type="email" {...bind('email')} maxLength={254} />}
          </FormField>
          <FormField label="Phone" name="phone" error={error}>
            {(p) => <input {...p} type="tel" {...bind('phone')} maxLength={30} />}
          </FormField>
          <FormField label="Team" name="teamId" error={error} required>
            {(p) => (
              <select {...p} {...bind('teamId')}>
                <option value="">Choose a team</option>
                {(reference.data?.teams ?? []).map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            )}
          </FormField>
          <FormField label="Region" name="regionId" error={error} required>
            {(p) => (
              <select {...p} {...bind('regionId')}>
                <option value="">Choose a region</option>
                {(reference.data?.regions ?? []).map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            )}
          </FormField>
          <FormField
            label="Linked sign-in account"
            name="linkedUserId"
            error={error}
            hint="Optional. Only RECRUITER accounts not linked to another recruiter are listed."
          >
            {(p) => (
              <select {...p} {...bind('linkedUserId')}>
                <option value="">Not linked</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.displayName} ({u.username})
                  </option>
                ))}
              </select>
            )}
          </FormField>
          <div className="form-actions">
            <button type="submit" className="primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
            <Link className="button" to={editing ? `/recruiters/${id}` : '/recruiters'}>
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </PageLayout>
  );
}
