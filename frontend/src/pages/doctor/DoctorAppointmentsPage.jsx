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

export default function DoctorAppointmentsPage() {
  const { doctor } = useDoctorPortal();
  const loadAppointments = useCallback(async () => {
    const [appointments, patients, encounters] = await Promise.all([
      doctorService.getAppointments(doctor.id),
      doctorService.getPatients(),
      doctorService.getDoctorEncounters(doctor.id),
    ]);
    return {
      appointments: [...appointments].sort((a, b) =>
        `${a.appointmentDate}T${a.appointmentTime}`.localeCompare(`${b.appointmentDate}T${b.appointmentTime}`),
      ),
      patients: Object.fromEntries(patients.map((patient) => [patient.id, patient])),
      encounterByAppointment: Object.fromEntries(encounters.map((encounter) => [encounter.appointmentId, encounter.id])),
    };
  }, [doctor.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadAppointments);

  return (
    <DoctorPage title="Appointments" description="Appointments assigned to your doctor profile.">
      {isLoading ? <LoadingState label="Loading your appointments..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title="Appointment schedule">
          {data.appointments.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                  <tr><th className="px-3 py-3">Patient</th><th className="px-3 py-3">Date</th><th className="px-3 py-3">Time</th><th className="px-3 py-3">Reason</th><th className="px-3 py-3">Status</th><th className="px-3 py-3">Action</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.appointments.map((appointment) => (
                    <tr key={appointment.id}>
                      <td className="px-3 py-4">
                        <Link className="font-medium text-indigo-700 hover:text-indigo-900" to={`/doctor/patients/${appointment.patientId}`}>
                          {patientName(data.patients[appointment.patientId])}
                        </Link>
                        <p className="mt-1 text-xs text-slate-500">Patient #{appointment.patientId}</p>
                      </td>
                      <td className="px-3 py-4">{formatDate(appointment.appointmentDate)}</td>
                      <td className="px-3 py-4">{appointment.appointmentTime || 'Not set'}</td>
                      <td className="max-w-xs whitespace-normal px-3 py-4">{appointment.reason || 'Not provided'}</td>
                      <td className="px-3 py-4"><StatusBadge>{appointment.status}</StatusBadge></td>
                      <td className="px-3 py-4">
                        {data.encounterByAppointment[appointment.id] ? (
                          <Link className="font-medium text-indigo-700 hover:text-indigo-900" to={`/doctor/encounters/${data.encounterByAppointment[appointment.id]}`}>
                            View encounter
                          </Link>
                        ) : (
                          <Link className="font-medium text-indigo-700 hover:text-indigo-900" to={`/doctor/encounters/new?appointmentId=${appointment.id}`}>
                            Start encounter
                          </Link>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState>No appointments are scheduled.</EmptyState>}
        </Panel>
      )}
    </DoctorPage>
  );
}
