import { useState, type FormEvent } from 'react';
import ErrorBanner from './ErrorBanner';
import FormField from './FormField';

interface Props {
  title: string;
  confirmLabel?: string;
  /** When true the reason must be non-blank before confirming. */
  required?: boolean;
  label?: string;
  error?: unknown;
  busy?: boolean;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
}

/** Modal asking for a reason (Hold, Inactive, Close, ...). */
export default function ReasonDialog({
  title,
  confirmLabel = 'Confirm',
  required = true,
  label = 'Reason',
  error,
  busy,
  onConfirm,
  onCancel,
}: Props) {
  const [reason, setReason] = useState('');
  const blank = reason.trim() === '';

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (required && blank) return;
    onConfirm(reason.trim());
  };

  return (
    <div className="dialog-backdrop" role="presentation">
      <form className="dialog" role="dialog" aria-modal="true" aria-label={title} onSubmit={submit}>
        <h2>{title}</h2>
        <ErrorBanner error={error} />
        <FormField label={label} name="reason" error={error} required={required}>
          {(p) => <textarea {...p} value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)} autoFocus />}
        </FormField>
        <div className="form-actions">
          <button type="submit" className="primary" disabled={busy || (required && blank)}>
            {confirmLabel}
          </button>
          <button type="button" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
