export function PatientPage({ eyebrow = 'Patient portal', title, description, children }) {
  return (
    <section className="mx-auto max-w-7xl px-4 py-7 sm:px-6 lg:px-8">
      <p className="text-sm font-semibold text-teal-700">{eyebrow}</p>
      <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">
        {title}
      </h1>
      {description && <p className="mt-2 max-w-3xl text-sm text-slate-600">{description}</p>}
      <div className="mt-7">{children}</div>
    </section>
  );
}

export function Panel({ title, action, children, className = '' }) {
  return (
    <section className={`rounded-2xl border border-slate-200 bg-white p-5 shadow-sm ${className}`}>
      {(title || action) && (
        <div className="mb-4 flex items-center justify-between gap-3">
          {title && <h2 className="font-semibold text-slate-900">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export function LoadingState({ label = 'Loading your information...' }) {
  return (
    <div className="flex min-h-40 items-center justify-center gap-3 text-sm text-slate-600" aria-live="polite">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-teal-700 border-r-transparent" />
      {label}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-800" role="alert">
      <p>{message}</p>
      {onRetry && (
        <button
          className="mt-3 rounded-lg border border-red-300 px-3 py-1.5 font-medium hover:bg-red-100"
          onClick={onRetry}
          type="button"
        >
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ children }) {
  return <p className="rounded-xl bg-slate-50 px-4 py-6 text-center text-sm text-slate-600">{children}</p>;
}

export function StatusBadge({ children }) {
  const status = String(children || 'Unknown').toUpperCase();
  const color = ['COMPLETED', 'CONFIRMED', 'ACTIVE'].includes(status)
    ? 'bg-emerald-50 text-emerald-700'
    : ['CANCELLED', 'CANCELED', 'REJECTED'].includes(status)
      ? 'bg-slate-100 text-slate-600'
      : status === 'URGENT' || status === 'STAT'
        ? 'bg-rose-50 text-rose-700'
        : 'bg-amber-50 text-amber-800';
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${color}`}>{status.replaceAll('_', ' ')}</span>;
}

export function formatDate(value, options = { year: 'numeric', month: 'short', day: 'numeric' }) {
  if (!value) return 'Not recorded';
  const date = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value);
  return Number.isNaN(date.getTime()) ? 'Not recorded' : date.toLocaleDateString(undefined, options);
}

export function currentDateString() {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${today.getFullYear()}-${month}-${day}`;
}

export function displayValue(value) {
  return value === null || value === undefined || value === '' ? 'Not on file' : value;
}

export function doctorName(doctor) {
  if (!doctor) return 'Care team';
  return [doctor.firstName, doctor.lastName].filter(Boolean).join(' ') || 'Care team';
}
