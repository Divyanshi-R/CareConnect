import { useCallback, useState } from 'react';
import adminService from '../../services/adminService.js';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import {
  AdminPage,
  EmptyState,
  ErrorState,
  formatDate,
  LoadingState,
  Panel,
  StatusBadge,
} from '../../components/admin/AdminPageUi.jsx';

export default function AdminAppointmentsPage() {
  const [actionError, setActionError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const loadAppointments = useCallback(async () => {
    const [appointments, patients, doctors] = await Promise.all([
      adminService.getAppointments(),
      adminService.getPatients(),
      adminService.getDoctors(),
    ]);
    return {
      appointments: [...appointments].sort((a, b) =>
        `${b.appointmentDate}T${b.appointmentTime}`.localeCompare(`${a.appointmentDate}T${a.appointmentTime}`),
      ),
      patients: Object.fromEntries(patients.map((patient) => [patient.id, patient])),
      doctors: Object.fromEntries(doctors.map((doctor) => [doctor.id, doctor])),
    };
  }, []);
  const { data, isLoading, error, reload } = useAsyncResource(loadAppointments);

  async function cancelAppointment(appointment) {
    if (!window.confirm(`Cancel appointment #${appointment.id}?`)) return;
    setActionError('');
    setBusyId(appointment.id);
    try {
      await adminService.cancelAppointment(appointment.id);
      reload();
    } catch (requestError) {
      setActionError(requestError.response?.data?.message || 'Unable to cancel this appointment.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <AdminPage title="Appointments" description="Appointments across the system, with patient and provider details.">
      {actionError && <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{actionError}</div>}
      {isLoading ? <LoadingState label="Loading appointments..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title={`${data.appointments.length} appointment${data.appointments.length === 1 ? '' : 's'}`}>
          {data.appointments.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                  <tr><th className="px-3 py-3">Patient</th><th className="px-3 py-3">Doctor</th><th className="px-3 py-3">Date</th><th className="px-3 py-3">Time</th><th className="px-3 py-3">Reason</th><th className="px-3 py-3">Status</th><th className="px-3 py-3">Action</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.appointments.map((appointment) => {
                    const patient = data.patients[appointment.patientId];
                    const doctor = data.doctors[appointment.doctorId];
                    return (
                      <tr key={appointment.id}>
                        <td className="px-3 py-4">{[patient?.firstName, patient?.lastName].filter(Boolean).join(' ') || `Patient #${appointment.patientId}`}</td>
                        <td className="px-3 py-4">{[doctor?.firstName, doctor?.lastName].filter(Boolean).join(' ') || `Doctor #${appointment.doctorId}`}</td>
                        <td className="px-3 py-4">{formatDate(appointment.appointmentDate)}</td>
                        <td className="px-3 py-4">{appointment.appointmentTime || 'Not set'}</td>
                        <td className="max-w-xs whitespace-normal px-3 py-4">{appointment.reason || 'Not provided'}</td>
                        <td className="px-3 py-4"><StatusBadge>{appointment.status}</StatusBadge></td>
                        <td className="px-3 py-4">
                          {!['CANCELLED', 'COMPLETED'].includes(appointment.status) && (
                            <button
                              className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
                              disabled={busyId === appointment.id}
                              onClick={() => cancelAppointment(appointment)}
                              type="button"
                            >
                              {busyId === appointment.id ? 'Cancelling...' : 'Cancel'}
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : <EmptyState>No appointments are available.</EmptyState>}
        </Panel>
      )}
    </AdminPage>
  );
}
