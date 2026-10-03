import { useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { currentDateString, EmptyState, ErrorState, formatDate, LoadingState, Panel, PatientPage, StatusBadge, doctorName } from '../../components/patient/PatientPageUi.jsx';
import { usePatientPortal } from '../../context/PatientPortalContext.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import patientService from '../../services/patientService.js';

function StatCard({ label, value, href }) {
  return (
    <Link className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-teal-300" to={href}>
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-slate-950">{value}</p>
    </Link>
  );
}

export default function PatientDashboardPage() {
  const { patient } = usePatientPortal();
  const loadDashboard = useCallback(async () => {
    const [appointments, encounters, prescriptions, medications, orders] = await Promise.all([
      patientService.getAppointments(patient.id),
      patientService.getEncounters(patient.id),
      patientService.getPrescriptions(patient.id),
      patientService.getMyMedications(),
      patientService.getOrders(patient.id),
    ]);
    const today = currentDateString();
    const upcoming = appointments
      .filter((appointment) => appointment.appointmentDate >= today)
      .filter((appointment) => !['CANCELLED', 'CANCELED'].includes(appointment.status))
      .sort((a, b) => `${a.appointmentDate}T${a.appointmentTime}`.localeCompare(`${b.appointmentDate}T${b.appointmentTime}`));
    const nextAppointment = upcoming[0] || null;
    const doctor = nextAppointment ? await patientService.getDoctor(nextAppointment.doctorId) : null;
    return { appointments, encounters, prescriptions, medications, orders, nextAppointment, appointmentDoctor: doctor };
  }, [patient.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadDashboard);
  const activePrescriptions = useMemo(
    () => data?.prescriptions.filter((prescription) => prescription.status === 'ACTIVE') || [],
    [data],
  );
  const pendingOrders = useMemo(
    () => data?.orders.filter((order) => !['COMPLETED', 'CANCELLED', 'CANCELED'].includes(order.status)) || [],
    [data],
  );
  const firstName = patient.firstName || 'there';

  return (
    <PatientPage title={`Welcome, ${firstName}`} description="Here is a summary of your care and the latest updates.">
      {isLoading ? <LoadingState label="Loading your dashboard..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Appointments" value={data.appointments.length} href="/patient/appointments" />
            <StatCard label="Active prescriptions" value={activePrescriptions.length} href="/patient/prescriptions" />
            <StatCard label="Active medications" value={data.medications.length} href="/patient/medications" />
            <StatCard label="Pending clinical orders" value={pendingOrders.length} href="/patient/orders" />
          </div>
          <div className="grid gap-6 xl:grid-cols-2">
            <Panel title="Upcoming appointment">
              {data.nextAppointment ? (
                <div>
                  <p className="text-lg font-semibold text-slate-950">
                    {formatDate(data.nextAppointment.appointmentDate)} · {data.nextAppointment.appointmentTime || 'Time not set'}
                  </p>
                  <p className="mt-1 text-sm text-slate-600">Dr. {doctorName(data.appointmentDoctor)}</p>
                  <p className="mt-2 text-sm text-slate-600">{data.nextAppointment.reason || 'Appointment'}</p>
                  <div className="mt-3"><StatusBadge>{data.nextAppointment.status}</StatusBadge></div>
                </div>
              ) : <EmptyState>No upcoming appointments are scheduled.</EmptyState>}
            </Panel>
            <Panel title="Recent encounters" action={<Link className="text-sm font-medium text-teal-700 hover:text-teal-900" to="/patient/medical-records">View records</Link>}>
              {data.encounters.length ? (
                <ul className="divide-y divide-slate-100">
                  {[...data.encounters].sort((a, b) => String(b.encounterDate).localeCompare(String(a.encounterDate))).slice(0, 3).map((encounter) => (
                    <li className="py-3 first:pt-0 last:pb-0" key={encounter.id}>
                      <p className="font-medium text-slate-900">{encounter.chiefComplaint || encounter.diagnosis || 'Clinical encounter'}</p>
                      <p className="mt-1 text-sm text-slate-500">{formatDate(encounter.encounterDate)}</p>
                    </li>
                  ))}
                </ul>
              ) : <EmptyState>No encounter history is available.</EmptyState>}
            </Panel>
          </div>
        </div>
      )}
    </PatientPage>
  );
}
