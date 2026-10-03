import { useCallback } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
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
} from '../../components/doctor/DoctorPageUi.jsx';
import { EncounterActionForms, EncounterEditor, StartEncounterForm } from './DoctorEncounterForms.jsx';

export default function DoctorEncounterPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const { doctor } = useDoctorPortal();
  const navigate = useNavigate();
  const isNew = id === 'new';
  const appointmentId = searchParams.get('appointmentId');

  const loadEncounter = useCallback(async () => {
    if (isNew) {
      const requestedAppointmentId = Number(appointmentId);
      if (!Number.isSafeInteger(requestedAppointmentId) || requestedAppointmentId <= 0) {
        throw new Error('Choose an appointment before starting an encounter.');
      }
      const appointment = await doctorService.getAppointment(requestedAppointmentId);
      if (appointment.doctorId !== doctor.id) {
        throw new Error('This appointment is not assigned to your doctor profile.');
      }
      const patient = await doctorService.getPatient(appointment.patientId);
      return { appointment, patient };
    }

    const encounterId = Number(id);
    if (!Number.isSafeInteger(encounterId) || encounterId <= 0) {
      throw new Error('The encounter ID is invalid.');
    }
    const encounter = await doctorService.getEncounter(encounterId);
    if (encounter.doctorId !== doctor.id) {
      throw new Error('This encounter is not assigned to your doctor profile.');
    }
    const [patient, notes, diagnoses, medications] = await Promise.all([
      doctorService.getPatient(encounter.patientId),
      doctorService.getEncounterNotes(encounter.id),
      doctorService.getEncounterDiagnoses(encounter.id),
      doctorService.getMedications(),
    ]);
    return { encounter, patient, notes, diagnoses, medications };
  }, [appointmentId, doctor.id, id, isNew]);

  const { data, isLoading, error, reload } = useAsyncResource(loadEncounter);
  const title = data?.patient ? `Encounter · ${patientName(data.patient)}` : isNew ? 'Start encounter' : 'Encounter';

  return (
    <DoctorPage title={title} description={data?.encounter ? `Encounter #${data.encounter.id} · ${formatDate(data.encounter.encounterDate)}` : 'Record clinical details for an appointment.'}>
      {isLoading ? <LoadingState label="Loading encounter..." /> : error ? <ErrorState message={error} onRetry={reload} /> : isNew ? (
        <div className="space-y-5">
          <Panel title="Appointment">
            <p className="font-medium text-slate-900">{formatDate(data.appointment.appointmentDate)} · {data.appointment.appointmentTime}</p>
            <p className="mt-1 text-sm text-slate-600">{data.appointment.reason || 'No reason provided'}</p>
            <p className="mt-1 text-xs text-slate-500">Appointment #{data.appointment.id}</p>
          </Panel>
          <StartEncounterForm
            appointment={data.appointment}
            doctor={doctor}
            onCreated={(created) => navigate(`/doctor/encounters/${created.id}`, { replace: true })}
            patient={data.patient}
          />
        </div>
      ) : (
        <div className="space-y-5">
          <Panel title="Patient and appointment">
            <p className="font-medium text-slate-900">{patientName(data.patient)} · Patient #{data.patient.id}</p>
            <p className="mt-1 text-sm text-slate-600">
              Appointment #{data.encounter.appointmentId} · <Link className="font-medium text-indigo-700" to="/doctor/appointments">Return to appointments</Link>
            </p>
          </Panel>
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            The encounter API does not provide a completion status or lifecycle endpoint. Saving updates the clinical content; it cannot mark the encounter complete.
          </div>
          <EncounterEditor encounter={data.encounter} key={data.encounter.id} onSaved={reload} />
          <Panel title="Structured diagnoses">
            {data.diagnoses.length ? data.diagnoses.map((diagnosis) => (
              <p className="border-b border-slate-100 py-3 text-sm last:border-0" key={diagnosis.id}>
                <strong>{diagnosis.diagnosisName}</strong> ({diagnosis.diagnosisCode}) · {diagnosis.status}
                <br />{diagnosis.description}
              </p>
            )) : <EmptyState>No structured diagnoses have been added to this encounter.</EmptyState>}
          </Panel>
          <Panel title="Clinical notes">
            {data.notes.length ? data.notes.map((note) => (
              <article className="border-b border-slate-100 py-3 last:border-0" key={note.id}>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{String(note.noteType).replaceAll('_', ' ')} · {formatDate(note.createdAt)}</p>
                <p className="mt-2 whitespace-pre-wrap text-sm text-slate-700">{note.content}</p>
              </article>
            )) : <EmptyState>No clinical notes have been added.</EmptyState>}
          </Panel>
          <EncounterActionForms encounter={data.encounter} medications={data.medications} onSaved={reload} />
        </div>
      )}
    </DoctorPage>
  );
}
