import type { ReactNode } from 'react';
import { fieldErrorFor } from '../api/errors';

export interface FieldControlProps {
  id: string;
  name: string;
  'aria-invalid': boolean;
  'aria-describedby'?: string;
}

interface Props {
  label: string;
  name: string;
  /** The last API error; its `fieldErrors` entry for `name` renders inline. */
  error?: unknown;
  required?: boolean;
  hint?: string;
  children: (control: FieldControlProps) => ReactNode;
}

/** Label + control + inline server field error (FR-104). */
export default function FormField({ label, name, error, required, hint, children }: Props) {
  const id = `field-${name}`;
  const message = fieldErrorFor(error, name);
  const describedBy = message ? `${id}-error` : hint ? `${id}-hint` : undefined;
  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {required && <span className="field__required"> *</span>}
      </label>
      {children({ id, name, 'aria-invalid': Boolean(message), 'aria-describedby': describedBy })}
      {hint && !message && (
        <span id={`${id}-hint`} className="field__hint">
          {hint}
        </span>
      )}
      {message && (
        <span id={`${id}-error`} className="field__error" role="alert">
          {message}
        </span>
      )}
    </div>
  );
}
