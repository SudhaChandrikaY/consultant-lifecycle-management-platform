import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { consultantsApi } from '../../api/consultants';
import { referenceApi } from '../../api/reference';
import type { ConsultantRequest, VisaType } from '../../api/types';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import LoadingState from '../../components/LoadingState';
import PageLayout from '../../components/PageLayout';
import { useApi } from '../../hooks/useApi';
import { VISA_TYPE_LABELS } from '../../labels';
import { fieldErrorsOf } from '../../api/errors';

interface FormState {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  city: string;
  state: string;
  primarySkill: string;
  additionalSkills: string;
  yearsExperience: string;
  visaType: string;
  visaExpirationDate: string;
  notes: string;
}

const EMPTY: FormState = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  city: '',
  state: '',
  primarySkill: '',
  additionalSkills: '',
  yearsExperience: '',
  visaType: '',
  visaExpirationDate: '',
  notes: '',
};

const orNull = (v: string) => (v.trim() === '' ? null : v.trim());

/** Create (/consultants/new) and edit (/consultants/:id/edit) a consultant profile. */
export default function ConsultantFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const reference = useApi(() => referenceApi.get(), []);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [version, setVersion] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(editing);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [error, setError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!id) return;
    consultantsApi
      .get(id)
      .then((c) => {
        setForm({
          firstName: c.firstName,
          lastName: c.lastName,
          email: c.contact?.email ?? '',
          phone: c.contact?.phone ?? '',
          city: c.city ?? '',
          state: c.state ?? '',
          primarySkill: c.primarySkill ?? '',
          additionalSkills: c.additionalSkills ?? '',
          yearsExperience: c.yearsExperience?.toString() ?? '',
          visaType: c.visaType ?? '',
          visaExpirationDate: c.contact?.visaExpirationDate ?? '',
          notes: c.contact?.notes ?? '',
        });
        setVersion(c.version);
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
    const years = form.yearsExperience.trim();
    const body: ConsultantRequest = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim(),
      phone: orNull(form.phone),
      city: orNull(form.city),
      state: orNull(form.state),
      primarySkill: orNull(form.primarySkill),
      additionalSkills: orNull(form.additionalSkills),
      yearsExperience: years === '' ? null : Number(years),
      visaType: (orNull(form.visaType) as VisaType | null) ?? null,
      visaExpirationDate: orNull(form.visaExpirationDate),
      notes: orNull(form.notes),
      version,
    };
    try {
      const saved = editing ? await consultantsApi.update(id!, body) : await consultantsApi.create(body);
      navigate(`/consultants/${saved.id}`);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  const title = editing ? 'Edit consultant' : 'Add consultant';
  if (loading) return <PageLayout title={title}><LoadingState /></PageLayout>;
  if (loadError) return <PageLayout title={title}><ErrorBanner error={loadError} /></PageLayout>;

  // Field errors render inline; only show the banner for errors without them.
  const bannerError = error && fieldErrorsOf(error).length === 0 ? error : null;

  return (
    <PageLayout title={title}>
      <div className="card">
        <ErrorBanner error={bannerError} />
        {Boolean(error) && !bannerError && (
          <div className="banner banner--error" role="alert">
            Please fix the highlighted fields.
          </div>
        )}
        <form className="form" onSubmit={onSubmit} noValidate style={{ maxWidth: 'none' }}>
          <div className="form-grid">
            <FormField label="First name" name="firstName" error={error} required>
              {(p) => <input {...p} {...bind('firstName')} maxLength={60} />}
            </FormField>
            <FormField label="Last name" name="lastName" error={error} required>
              {(p) => <input {...p} {...bind('lastName')} maxLength={60} />}
            </FormField>
            <FormField label="Email" name="email" error={error} required>
              {(p) => <input {...p} type="email" {...bind('email')} maxLength={254} />}
            </FormField>
            <FormField label="Phone" name="phone" error={error} hint="Required before Ready">
              {(p) => <input {...p} type="tel" {...bind('phone')} maxLength={30} />}
            </FormField>
            <FormField label="City" name="city" error={error}>
              {(p) => <input {...p} {...bind('city')} maxLength={80} />}
            </FormField>
            <FormField label="State" name="state" error={error}>
              {(p) => <input {...p} {...bind('state')} maxLength={40} />}
            </FormField>
            <FormField label="Primary skill" name="primarySkill" error={error} hint="Required before Ready">
              {(p) => <input {...p} {...bind('primarySkill')} maxLength={80} />}
            </FormField>
            <FormField label="Additional skills" name="additionalSkills" error={error}>
              {(p) => <input {...p} {...bind('additionalSkills')} maxLength={500} />}
            </FormField>
            <FormField label="Years of experience" name="yearsExperience" error={error} hint="0–50; required before Ready">
              {(p) => <input {...p} type="number" min={0} max={50} step={1} {...bind('yearsExperience')} />}
            </FormField>
            <FormField label="Visa type" name="visaType" error={error} hint="Required before Ready">
              {(p) => (
                <select {...p} {...bind('visaType')}>
                  <option value="">Not set</option>
                  {(reference.data?.visaTypes ?? []).map((v) => (
                    <option key={v} value={v}>
                      {VISA_TYPE_LABELS[v]}
                    </option>
                  ))}
                </select>
              )}
            </FormField>
            <FormField label="Visa expiration date" name="visaExpirationDate" error={error}>
              {(p) => <input {...p} type="date" {...bind('visaExpirationDate')} />}
            </FormField>
          </div>
          <FormField label="Notes" name="notes" error={error}>
            {(p) => <textarea {...p} {...bind('notes')} maxLength={2000} />}
          </FormField>
          <div className="form-actions">
            <button type="submit" className="primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
            <Link className="button" to={editing ? `/consultants/${id}` : '/consultants'}>
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </PageLayout>
  );
}
