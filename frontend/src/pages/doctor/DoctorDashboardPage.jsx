import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useDoctorPortal } from '../../context/DoctorPortalContext.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import doctorService from '../../services/doctorService.js';
import {
  currentDateString,
  DoctorPage,
  EmptyState,
  ErrorState,
  formatDate,
  LoadingState,
  Panel,
  patientName,
  StatusBadge,
} from '../../components/doctor/DoctorPageUi.jsx';

function CountCard({ label, value, to }) {
  return (
    <Link className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-indigo-300" to={to}>
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-slate-950">{value}</p>
    </Link>
  );
}

export default function DoctorDashboardPage() {
  const { doctor } = useDoctorPortal();
  const loadDashboard = useCallback(async () => {
    const [appointments, patients, orders, encounters] = await Promise.all([
      doctorService.getAppointments(doctor.id),
      doctorService.getPatients(),
      doctorService.getOrders(doctor.id),
      doctorService.getDoctorEncounters(doctor.id),
    ]);
    const people = Object.fromEntries(patients.map((patient) => [patient.id, patient]));
    const today = currentDateString();
    const futureAppointments = appointments
      .filter((appointment) => appointment.appointmentDate >= today)
      .filter((appointment) => !['CANCELLED', 'CANCELED'].includes(appointment.status))
      .sort((a, b) => `${a.appointmentDate}T${a.appointmentTime}`.localeCompare(`${b.appointmentDate}T${b.appointmentTime}`));
    return {
      appointments,
      patients,
      orders,
      encounters: [...encounters].sort((a, b) => String(b.encounterDate).localeCompare(String(a.encounterDate))),
      people,
      todayAppointments: futureAppointments.filter((appointment) => appointment.appointmentDate === today),
      upcomingAppointments: futureAppointments,
      pendingOrders: orders.filter((order) => !['COMPLETED', 'CANCELLED'].includes(order.status)),
    };
  }, [doctor.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadDashboard);

  return (
    <DoctorPage title={`Welcome, Dr. ${doctor.lastName || doctor.firstName}`} description="Your appointments, patients, and clinical work at a glance.">
      {isLoading ? <LoadingState label="Loading your dashboard..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <CountCard label="Today's appointments" value={data.todayAppointments.length} to="/doctor/appointments" />
            <CountCard label="Upcoming appointments" value={data.upcomingAppointments.length} to="/doctor/appointments" />
            <CountCard label="Patients" value={data.patients.length} to="/doctor/patients" />
            <CountCard label="Pending clinical orders" value={data.pendingOrders.length} to="/doctor/orders" />
          </div>
          <div className="grid gap-6 xl:grid-cols-2">
            <Panel title="Today's appointments" action={<Link className="text-sm font-medium text-indigo-700" to="/doctor/appointments">All appointments</Link>}>
              {data.todayAppointments.length ? (
                <ul className="divide-y divide-slate-100">
                  {data.todayAppointments.slice(0, 5).map((appointment) => (
                    <li className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0" key={appointment.id}>
                      <div>
                        <Link className="font-medium text-slate-900 hover:text-indigo-700" to={`/doctor/patients/${appointment.patientId}`}>{patientName(data.people[appointment.patientId])}</Link>
                        <p className="mt-1 text-sm text-slate-500">{appointment.appointmentTime || 'Time not set'} · {appointment.reason || 'Appointment'}</p>
                      </div>
                      <StatusBadge>{appointment.status}</StatusBadge>
                    </li>
                  ))}
                </ul>
              ) : <EmptyState>No appointments are scheduled for today.</EmptyState>}
            </Panel>
            <Panel title="Recent encounters">
              {data.encounters.length ? (
                <ul className="divide-y divide-slate-100">
                  {data.encounters.slice(0, 5).map((encounter) => (
                    <li className="py-3 first:pt-0 last:pb-0" key={encounter.id}>
                      <Link className="font-medium text-slate-900 hover:text-indigo-700" to={`/doctor/encounters/${encounter.id}`}>
                        {patientName(data.people[encounter.patientId])} · {encounter.chiefComplaint || 'Clinical encounter'}
                      </Link>
                      <p className="mt-1 text-sm text-slate-500">{formatDate(encounter.encounterDate)}</p>
                    </li>
                  ))}
                </ul>
              ) : <EmptyState>No encounter history is available.</EmptyState>}
            </Panel>
          </div>
          <Panel title="Upcoming appointments">
            {data.upcomingAppointments.length ? (
              <ul className="divide-y divide-slate-100">
                {data.upcomingAppointments.slice(0, 6).map((appointment) => (
                  <li className="flex flex-col gap-2 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between" key={appointment.id}>
                    <div>
                      <Link className="font-medium text-slate-900 hover:text-indigo-700" to={`/doctor/patients/${appointment.patientId}`}>{patientName(data.people[appointment.patientId])}</Link>
                      <p className="mt-1 text-sm text-slate-500">{appointment.reason || 'Appointment'}</p>
                    </div>
                    <p className="text-sm text-slate-600">{formatDate(appointment.appointmentDate)} · {appointment.appointmentTime}</p>
                  </li>
                ))}
              </ul>
            ) : <EmptyState>No upcoming appointments.</EmptyState>}
          </Panel>
        </div>
      )}
    </DoctorPage>
  );
}
