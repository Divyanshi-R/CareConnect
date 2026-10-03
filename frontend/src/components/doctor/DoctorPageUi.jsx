import {
  EmptyState,
  ErrorState,
  LoadingState,
  Panel,
  PatientPage,
  StatusBadge,
  currentDateString,
  displayValue,
  doctorName,
  formatDate,
} from '../patient/PatientPageUi.jsx';

export { EmptyState, ErrorState, LoadingState, Panel, StatusBadge, currentDateString, displayValue, doctorName, formatDate };

export function DoctorPage({ title, description, children }) {
  return (
    <PatientPage eyebrow="Doctor workspace" title={title} description={description}>
      {children}
    </PatientPage>
  );
}

export function ApiFormError({ error }) {
  if (!error) return null;
  const response = error.response?.data;
  const validationErrors = response?.validationErrors;
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">
      <p className="font-medium">{response?.message || error.message || 'The request could not be completed.'}</p>
      {validationErrors && (
        <ul className="mt-2 list-inside list-disc space-y-1">
          {Object.entries(validationErrors).map(([field, message]) => (
            <li key={field}><strong>{field}:</strong> {message}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

const inputClass = 'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100';

export function FormField({ label, name, value, onChange, type = 'text', required = false, maxLength, min, step, placeholder }) {
  return (
    <label className="block text-sm font-medium text-slate-700" htmlFor={name}>
      {label}
      <input
        className={inputClass}
        id={name}
        maxLength={maxLength}
        min={min}
        name={name}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        step={step}
        type={type}
        value={value}
      />
    </label>
  );
}

export function FormTextArea({ label, name, value, onChange, required = false, maxLength, rows = 3 }) {
  return (
    <label className="block text-sm font-medium text-slate-700" htmlFor={name}>
      {label}
      <textarea
        className={inputClass}
        id={name}
        maxLength={maxLength}
        name={name}
        onChange={onChange}
        required={required}
        rows={rows}
        value={value}
      />
    </label>
  );
}

export function FormSelect({ label, name, value, onChange, options, required = false }) {
  return (
    <label className="block text-sm font-medium text-slate-700" htmlFor={name}>
      {label}
      <select className={inputClass} id={name} name={name} onChange={onChange} required={required} value={value}>
        {options.map(([optionValue, optionLabel]) => (
          <option key={optionValue} value={optionValue}>{optionLabel}</option>
        ))}
      </select>
    </label>
  );
}

export function SubmitButton({ children, disabled = false }) {
  return (
    <button
      className="rounded-lg bg-indigo-700 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-800 disabled:cursor-not-allowed disabled:opacity-60"
      disabled={disabled}
      type="submit"
    >
      {children}
    </button>
  );
}

export function patientName(patient) {
  if (!patient) return 'Patient';
  return [patient.firstName, patient.lastName].filter(Boolean).join(' ') || 'Patient';
}
