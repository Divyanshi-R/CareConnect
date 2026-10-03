import { Link } from 'react-router-dom';
import {
  EmptyState,
  ErrorState,
  formatDate,
  LoadingState,
  Panel,
  StatusBadge,
} from '../patient/PatientPageUi.jsx';

export { EmptyState, ErrorState, formatDate, LoadingState, Panel, StatusBadge };

export function AdminPage({ title, description, children }) {
  return (
    <section className="mx-auto max-w-7xl px-4 py-7 sm:px-6 lg:px-8">
      <p className="text-sm font-semibold text-slate-700">Administration</p>
      <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">{title}</h1>
      {description && <p className="mt-2 max-w-3xl text-sm text-slate-600">{description}</p>}
      <div className="mt-7">{children}</div>
    </section>
  );
}

export function ApiError({ error }) {
  if (!error) return null;
  const response = error.response?.data;
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">
      <p className="font-medium">{response?.message || error.message || 'The request could not be completed.'}</p>
      {response?.validationErrors && (
        <ul className="mt-2 list-inside list-disc space-y-1">
          {Object.entries(response.validationErrors).map(([field, message]) => (
            <li key={field}><strong>{field}:</strong> {message}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export const adminInputClass = 'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-700 focus:ring-2 focus:ring-slate-200';

export function MetricCard({ label, value, to }) {
  return (
    <Link className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-400" to={to}>
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-slate-950">{value}</p>
    </Link>
  );
}
