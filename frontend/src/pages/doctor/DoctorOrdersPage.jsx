import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useDoctorPortal } from '../../context/DoctorPortalContext.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import doctorService from '../../services/doctorService.js';
import {
  DoctorPage,
  EmptyState,
  ErrorState,
  formatDate,
  LoadingState,
  Panel,
  patientName,
  StatusBadge,
} from '../../components/doctor/DoctorPageUi.jsx';

export default function DoctorOrdersPage() {
  const { doctor } = useDoctorPortal();
  const loadOrders = useCallback(async () => {
    const [orders, patients] = await Promise.all([
      doctorService.getOrders(doctor.id),
      doctorService.getPatients(),
    ]);
    return {
      orders: [...orders].sort((a, b) => String(b.orderedAt).localeCompare(String(a.orderedAt))),
      patients: Object.fromEntries(patients.map((patient) => [patient.id, patient])),
    };
  }, [doctor.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadOrders);

  return (
    <DoctorPage title="Clinical orders" description="Monitor clinical orders attached to your encounters.">
      {isLoading ? <LoadingState label="Loading clinical orders..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title="Order history">
          {data.orders.length ? (
            <div className="divide-y divide-slate-100">
              {data.orders.map((order) => (
                <article className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between" key={order.id}>
                  <div>
                    <Link className="font-semibold text-indigo-700 hover:text-indigo-900" to={`/doctor/patients/${order.patientId}`}>
                      {patientName(data.patients[order.patientId])}
                    </Link>
                    <p className="mt-1 text-sm text-slate-600">{order.orderType}: {order.description}</p>
                    <p className="mt-1 text-xs text-slate-500">Patient #{order.patientId} · Ordered {formatDate(order.orderedAt)}</p>
                    <Link className="mt-2 inline-block text-sm font-medium text-indigo-700" to={`/doctor/encounters/${order.encounterId}`}>Open encounter</Link>
                  </div>
                  <div className="flex gap-2">
                    <StatusBadge>{order.priority}</StatusBadge>
                    <StatusBadge>{order.status}</StatusBadge>
                  </div>
                </article>
              ))}
            </div>
          ) : <EmptyState>No clinical orders are available.</EmptyState>}
        </Panel>
      )}
    </DoctorPage>
  );
}
