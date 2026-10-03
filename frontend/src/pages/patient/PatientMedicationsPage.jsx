import { useCallback } from 'react';
import { EmptyState, ErrorState, LoadingState, Panel, PatientPage } from '../../components/patient/PatientPageUi.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import patientService from '../../services/patientService.js';

export default function PatientMedicationsPage() {
  const loadMedications = useCallback(() => patientService.getMyMedications(), []);
  const { data, isLoading, error, reload } = useAsyncResource(loadMedications);

  return (
    <PatientPage title="Medications" description="Your active medications from current prescriptions.">
      {isLoading ? <LoadingState label="Loading your medications..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title="Active medications">
          {data.length ? (
            <ul className="grid gap-4 md:grid-cols-2">
              {data.map((medication) => (
                <li className="rounded-xl border border-slate-100 p-4" key={medication.id}>
                  <h3 className="font-semibold text-slate-950">{medication.name}</h3>
                  {medication.genericName && <p className="mt-1 text-sm text-slate-600">{medication.genericName}</p>}
                  <p className="mt-2 text-sm text-slate-600">{[medication.strength, medication.form].filter(Boolean).join(' · ') || 'Strength not recorded'}</p>
                  {medication.description && <p className="mt-3 text-sm text-slate-600">{medication.description}</p>}
                </li>
              ))}
            </ul>
          ) : <EmptyState>You have no active medications on file.</EmptyState>}
        </Panel>
      )}
    </PatientPage>
  );
}
