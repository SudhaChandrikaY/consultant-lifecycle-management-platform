import { useState, type FormEvent } from 'react';
import { consultantsApi } from '../../api/consultants';
import { recruitersApi } from '../../api/recruiters';
import type { ConsultantDetail } from '../../api/types';
import ErrorBanner from '../../components/ErrorBanner';
import FormField from '../../components/FormField';
import LoadingState from '../../components/LoadingState';
import { useApi } from '../../hooks/useApi';

interface Props {
  consultant: ConsultantDetail;
  onAssigned: (updated: ConsultantDetail) => void;
  onCancel: () => void;
}

/** Assign or reassign the consultant's primary recruiter; offers active recruiters only (FR-034). */
export default function AssignRecruiterDialog({ consultant, onAssigned, onCancel }: Props) {
  const recruiters = useApi(() => recruitersApi.active(), []);
  const [recruiterId, setRecruiterId] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!recruiterId) return;
    setBusy(true);
    setError(null);
    try {
      onAssigned(
        await consultantsApi.assignRecruiter(consultant.id, { recruiterId: Number(recruiterId), version: consultant.version }),
      );
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const options = (recruiters.data?.items ?? []).filter((r) => r.id !== consultant.assignedRecruiter?.id);

  return (
    <div className="dialog-backdrop" role="presentation">
      <form className="dialog" role="dialog" aria-modal="true" aria-label="Assign recruiter" onSubmit={submit}>
        <h2>{consultant.assignedRecruiter ? 'Reassign recruiter' : 'Assign recruiter'}</h2>
        {consultant.assignedRecruiter && (
          <p className="muted">Currently assigned to {consultant.assignedRecruiter.fullName}.</p>
        )}
        <ErrorBanner error={error} />
        {recruiters.loading ? (
          <LoadingState />
        ) : (
          <FormField label="Recruiter" name="recruiterId" error={error} required>
            {(p) => (
              <select {...p} value={recruiterId} onChange={(e) => setRecruiterId(e.target.value)} autoFocus>
                <option value="">Choose an active recruiter</option>
                {options.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.fullName} — {r.team.name}
                  </option>
                ))}
              </select>
            )}
          </FormField>
        )}
        <div className="form-actions">
          <button type="submit" className="primary" disabled={busy || !recruiterId}>
            Assign
          </button>
          <button type="button" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
