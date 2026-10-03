import { useCallback } from 'react';
import { EmptyState, ErrorState, formatDate, LoadingState, Panel, PatientPage, doctorName } from '../../components/patient/PatientPageUi.jsx';
import { usePatientPortal } from '../../context/PatientPortalContext.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import patientService from '../../services/patientService.js';

export default function PatientMedicalRecordsPage() {
  const { patient } = usePatientPortal();
  const loadRecords = useCallback(async () => {
    const [encounters, notes, diagnoses] = await Promise.all([
      patientService.getEncounters(patient.id),
      patientService.getMyNotes(),
      patientService.getDiagnoses(patient.id),
    ]);
    const doctors = await patientService.getDoctors(encounters);
    return { encounters, notes, diagnoses, doctors };
  }, [patient.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadRecords);

  return (
    <PatientPage title="Medical records" description="Review encounter history, diagnoses, care plans, and notes shared with you.">
      {isLoading ? <LoadingState label="Loading your medical records..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="space-y-6">
          <Panel title="Encounter history">
            {data.encounters.length ? (
              <div className="space-y-4">
                {[...data.encounters].sort((a, b) => String(b.encounterDate).localeCompare(String(a.encounterDate))).map((encounter) => {
                  const diagnoses = data.diagnoses.filter((diagnosis) => diagnosis.encounterId === encounter.id);
                  const notes = data.notes.filter((note) => note.encounterId === encounter.id);
                  return (
                    <article className="rounded-xl border border-slate-100 p-4" key={encounter.id}>
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <h3 className="font-semibold text-slate-950">{encounter.chiefComplaint || 'Clinical encounter'}</h3>
                          <p className="mt-1 text-sm text-slate-500">{formatDate(encounter.encounterDate)} · Dr. {doctorName(data.doctors[encounter.doctorId])}</p>
                        </div>
                      </div>
                      {encounter.diagnosis && <p className="mt-4 text-sm text-slate-700"><strong>Diagnosis:</strong> {encounter.diagnosis}</p>}
                      {diagnoses.length > 0 && (
                        <div className="mt-4">
                          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Diagnoses</p>
                          <ul className="mt-2 list-inside list-disc space-y-1 text-sm text-slate-700">
                            {diagnoses.map((diagnosis) => <li key={diagnosis.id}>{diagnosis.diagnosisName}{diagnosis.description ? ` — ${diagnosis.description}` : ''}</li>)}
                          </ul>
                        </div>
                      )}
                      {encounter.treatmentPlan && <p className="mt-4 text-sm text-slate-700"><strong>Treatment:</strong> {encounter.treatmentPlan}</p>}
                      {encounter.notes && <p className="mt-4 text-sm text-slate-700"><strong>Encounter notes:</strong> {encounter.notes}</p>}
                      {notes.length > 0 && (
                        <div className="mt-4 border-t border-slate-100 pt-3">
                          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Notes available to you</p>
                          {notes.map((note) => <p className="mt-2 whitespace-pre-wrap text-sm text-slate-700" key={note.id}>{note.content}</p>)}
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            ) : <EmptyState>No encounter history is available.</EmptyState>}
          </Panel>
          {!data.encounters.length && data.notes.length > 0 && (
            <Panel title="Clinical notes">
              {data.notes.map((note) => <p className="whitespace-pre-wrap border-b border-slate-100 py-3 text-sm last:border-0" key={note.id}>{note.content}</p>)}
            </Panel>
          )}
          {!data.encounters.length && data.diagnoses.length > 0 && (
            <Panel title="Diagnoses">
              {data.diagnoses.map((diagnosis) => <p className="border-b border-slate-100 py-3 text-sm last:border-0" key={diagnosis.id}>{diagnosis.diagnosisName}{diagnosis.description ? ` — ${diagnosis.description}` : ''}</p>)}
            </Panel>
          )}
        </div>
      )}
    </PatientPage>
  );
}
