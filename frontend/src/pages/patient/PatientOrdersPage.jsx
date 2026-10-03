import { useCallback } from 'react';
import { EmptyState, ErrorState, formatDate, LoadingState, Panel, PatientPage, StatusBadge } from '../../components/patient/PatientPageUi.jsx';
import { usePatientPortal } from '../../context/PatientPortalContext.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import patientService from '../../services/patientService.js';

export default function PatientOrdersPage() {
  const { patient } = usePatientPortal();
  const loadOrders = useCallback(() => patientService.getOrders(patient.id), [patient.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadOrders);

  return (
    <PatientPage title="Clinical orders" description="Track orders placed as part of your care.">
      {isLoading ? <LoadingState label="Loading your clinical orders..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title="Order history">
          {data.length ? (
            <div className="divide-y divide-slate-100">
              {[...data].sort((a, b) => String(b.orderedAt).localeCompare(String(a.orderedAt))).map((order) => (
                <article className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between" key={order.id}>
                  <div>
                    <h3 className="font-semibold text-slate-950">{String(order.orderType || 'Clinical order').replaceAll('_', ' ')}</h3>
                    <p className="mt-1 text-sm text-slate-600">{order.description || 'No description provided.'}</p>
                    <p className="mt-2 text-xs text-slate-500">Ordered {formatDate(order.orderedAt, { year: 'numeric', month: 'short', day: 'numeric' })}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge>{order.priority}</StatusBadge>
                    <StatusBadge>{order.status}</StatusBadge>
                  </div>
                </article>
              ))}
            </div>
          ) : <EmptyState>No clinical orders are available.</EmptyState>}
        </Panel>
      )}
    </PatientPage>
  );
}
