export default function FormField({ label, error, hint, id, ...inputProps }) {
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input id={id} {...inputProps} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined} />
      {hint && <small id={`${id}-hint`} className="field-hint">{hint}</small>}
      {error && <small id={`${id}-error`} className="field-error">{error}</small>}
    </div>
  );
}
