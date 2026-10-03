import { useCallback, useMemo } from 'react';
import { currentDateString, EmptyState, ErrorState, formatDate, LoadingState, Panel, PatientPage, StatusBadge, doctorName } from '../../components/patient/PatientPageUi.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import { usePatientPortal } from '../../context/PatientPortalContext.jsx';
import patientService from '../../services/patientService.js';

function AppointmentList({ appointments, doctors, emptyMessage }) {
  if (!appointments.length) return <EmptyState>{emptyMessage}</EmptyState>;
  return (
    <div className="divide-y divide-slate-100">
      {appointments.map((appointment) => (
        <article className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between" key={appointment.id}>
          <div>
            <p className="font-semibold text-slate-950">{appointment.reason || 'Appointment'}</p>
            <p className="mt-1 text-sm text-slate-600">Dr. {doctorName(doctors[appointment.doctorId])}</p>
            <p className="mt-1 text-sm text-slate-500">{formatDate(appointment.appointmentDate)} · {appointment.appointmentTime || 'Time not set'}</p>
            {appointment.notes && <p className="mt-2 text-sm text-slate-600">{appointment.notes}</p>}
          </div>
          <StatusBadge>{appointment.status}</StatusBadge>
        </article>
      ))}
    </div>
  );
}

export default function PatientAppointmentsPage() {
  const { patient } = usePatientPortal();
  const loadAppointments = useCallback(async () => {
    const appointments = await patientService.getAppointments(patient.id);
    const doctors = await patientService.getDoctors(appointments);
    const today = currentDateString();
    const upcoming = appointments
      .filter((appointment) => appointment.appointmentDate >= today && !['CANCELLED', 'CANCELED'].includes(appointment.status))
      .sort((a, b) => `${a.appointmentDate}T${a.appointmentTime}`.localeCompare(`${b.appointmentDate}T${b.appointmentTime}`));
    const previous = appointments
      .filter((appointment) => appointment.appointmentDate < today || ['CANCELLED', 'CANCELED'].includes(appointment.status))
      .sort((a, b) => `${b.appointmentDate}T${b.appointmentTime}`.localeCompare(`${a.appointmentDate}T${a.appointmentTime}`));
    return { doctors, upcoming, previous };
  }, [patient.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadAppointments);

  return (
    <PatientPage title="Appointments" description="See your scheduled visits and appointment history.">
      {isLoading ? <LoadingState label="Loading your appointments..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="space-y-6">
          <Panel title="Upcoming appointments">
            <AppointmentList appointments={data.upcoming} doctors={data.doctors} emptyMessage="You have no upcoming appointments." />
          </Panel>
          <Panel title="Previous appointments">
            <AppointmentList appointments={data.previous} doctors={data.doctors} emptyMessage="You have no previous appointments." />
          </Panel>
        </div>
      )}
    </PatientPage>
  );
}
