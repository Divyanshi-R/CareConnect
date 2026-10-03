import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import adminService from '../../services/adminService.js';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import {
  AdminPage,
  EmptyState,
  ErrorState,
  formatDate,
  LoadingState,
  MetricCard,
  Panel,
  StatusBadge,
} from '../../components/admin/AdminPageUi.jsx';

export default function AdminDashboardPage() {
  const loadSummary = useCallback(async () => {
    const [users, patients, doctors, departments, appointments] = await Promise.all([
      adminService.getUsers(),
      adminService.getPatients(),
      adminService.getDoctors(),
      adminService.getDepartments(),
      adminService.getAppointments(),
    ]);
    const patientById = Object.fromEntries(patients.map((patient) => [patient.id, patient]));
    const doctorById = Object.fromEntries(doctors.map((doctor) => [doctor.id, doctor]));
    const now = new Date();
    const upcoming = appointments
      .filter((appointment) => new Date(`${appointment.appointmentDate}T${appointment.appointmentTime || '00:00:00'}`) >= now)
      .filter((appointment) => !['CANCELLED'].includes(appointment.status))
      .sort((a, b) =>
        `${a.appointmentDate}T${a.appointmentTime}`.localeCompare(`${b.appointmentDate}T${b.appointmentTime}`),
      )
      .slice(0, 6);
    return { users, patients, doctors, departments, appointments, patientById, doctorById, upcoming };
  }, []);
  const { data, isLoading, error, reload } = useAsyncResource(loadSummary);

  return (
    <AdminPage title="Dashboard" description="An overview of CareConnect users, providers, and appointments.">
      {isLoading ? <LoadingState label="Loading administrative summary..." /> : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
            <MetricCard label="Total users" value={data.users.length} to="/admin/users" />
            <MetricCard label="Patients" value={data.patients.length} to="/admin/patients" />
            <MetricCard label="Doctors" value={data.doctors.length} to="/admin/doctors" />
            <MetricCard label="Departments" value={data.departments.length} to="/admin/departments" />
            <MetricCard label="Appointments" value={data.appointments.length} to="/admin/appointments" />
          </div>
          <Panel title="Upcoming appointments" action={<Link className="text-sm font-medium text-slate-800" to="/admin/appointments">All appointments</Link>}>
            {data.upcoming.length ? (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[680px] text-left text-sm">
                  <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                    <tr><th className="px-3 py-3">Patient</th><th className="px-3 py-3">Doctor</th><th className="px-3 py-3">Date and time</th><th className="px-3 py-3">Reason</th><th className="px-3 py-3">Status</th></tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {data.upcoming.map((appointment) => (
                      <tr key={appointment.id}>
                        <td className="px-3 py-4">{[data.patientById[appointment.patientId]?.firstName, data.patientById[appointment.patientId]?.lastName].filter(Boolean).join(' ') || `Patient #${appointment.patientId}`}</td>
                        <td className="px-3 py-4">{[data.doctorById[appointment.doctorId]?.firstName, data.doctorById[appointment.doctorId]?.lastName].filter(Boolean).join(' ') || `Doctor #${appointment.doctorId}`}</td>
                        <td className="px-3 py-4">{formatDate(appointment.appointmentDate)} · {appointment.appointmentTime}</td>
                        <td className="px-3 py-4">{appointment.reason || 'Not provided'}</td>
                        <td className="px-3 py-4"><StatusBadge>{appointment.status}</StatusBadge></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : <EmptyState>No upcoming appointments.</EmptyState>}
          </Panel>
        </div>
      )}
    </AdminPage>
  );
}
