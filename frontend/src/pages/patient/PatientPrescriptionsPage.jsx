import { useCallback } from 'react';
import { EmptyState, ErrorState, formatDate, LoadingState, Panel, PatientPage, StatusBadge, doctorName } from '../../components/patient/PatientPageUi.jsx';
import { usePatientPortal } from '../../context/PatientPortalContext.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import patientService from '../../services/patientService.js';

export default function PatientPrescriptionsPage() {
  const { patient } = usePatientPortal();
  const loadPrescriptions = useCallback(async () => {
    const [prescriptions, medications] = await Promise.all([
      patientService.getPrescriptions(patient.id),
      patientService.getMyMedications(),
    ]);
    const doctors = await patientService.getDoctors(prescriptions);
    return { prescriptions, medications, doctors };
  }, [patient.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadPrescriptions);
  const medicationNames = Object.fromEntries((data?.medications || []).map((medication) => [medication.id, medication.name]));

  return (
    <PatientPage title="Prescriptions" description="View the instructions and details recorded for your prescriptions.">
      {isLoading ? <LoadingState label="Loading your prescriptions..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title="Prescription history">
          {data.prescriptions.length ? (
            <div className="space-y-4">
              {[...data.prescriptions].sort((a, b) => String(b.prescribedDate).localeCompare(String(a.prescribedDate))).map((prescription) => (
                <article className="rounded-xl border border-slate-100 p-4" key={prescription.id}>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h3 className="font-semibold text-slate-950">Prescription · {formatDate(prescription.prescribedDate)}</h3>
                      <p className="mt-1 text-sm text-slate-600">Dr. {doctorName(data.doctors[prescription.doctorId])}</p>
                    </div>
                    <StatusBadge>{prescription.status}</StatusBadge>
                  </div>
                  {prescription.items?.length ? (
                    <ul className="mt-4 space-y-3">
                      {prescription.items.map((item) => (
                        <li className="rounded-lg bg-slate-50 p-3" key={item.id}>
                          <p className="font-medium text-slate-900">{medicationNames[item.medicationId] || `Medication #${item.medicationId}`}</p>
                          <p className="mt-1 text-sm text-slate-600">
                            {[
                              item.dosage && `Dosage: ${item.dosage}`,
                              item.frequency && `Frequency: ${item.frequency}`,
                              item.duration && `Duration: ${item.duration}`,
                            ].filter(Boolean).join(' · ') || 'No administration details recorded'}
                          </p>
                          {item.instructions && <p className="mt-1 text-sm text-slate-600">{item.instructions}</p>}
                        </li>
                      ))}
                    </ul>
                  ) : <p className="mt-4 text-sm text-slate-500">No medication details are recorded.</p>}
                  {prescription.instructions && <p className="mt-3 text-sm text-slate-700"><strong>Instructions:</strong> {prescription.instructions}</p>}
                </article>
              ))}
            </div>
          ) : <EmptyState>No prescriptions are available.</EmptyState>}
        </Panel>
      )}
    </PatientPage>
  );
}
