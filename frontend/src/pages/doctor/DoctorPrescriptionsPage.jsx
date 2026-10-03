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

export default function DoctorPrescriptionsPage() {
  const { doctor } = useDoctorPortal();
  const loadPrescriptions = useCallback(async () => {
    const [prescriptions, patients, medications] = await Promise.all([
      doctorService.getPrescriptions(doctor.id),
      doctorService.getPatients(),
      doctorService.getMedications(),
    ]);
    return {
      prescriptions: [...prescriptions].sort((a, b) => String(b.prescribedDate).localeCompare(String(a.prescribedDate))),
      patients: Object.fromEntries(patients.map((patient) => [patient.id, patient])),
      medications: Object.fromEntries(medications.map((medication) => [medication.id, medication])),
    };
  }, [doctor.id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadPrescriptions);

  return (
    <DoctorPage title="Prescriptions" description="Prescriptions created for your patients.">
      {isLoading ? <LoadingState label="Loading prescriptions..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title="Prescription history">
          {data.prescriptions.length ? (
            <div className="space-y-4">
              {data.prescriptions.map((prescription) => (
                <article className="rounded-xl border border-slate-100 p-4" key={prescription.id}>
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <Link className="font-semibold text-indigo-700 hover:text-indigo-900" to={`/doctor/patients/${prescription.patientId}`}>
                        {patientName(data.patients[prescription.patientId])}
                      </Link>
                      <p className="mt-1 text-sm text-slate-500">Patient #{prescription.patientId} · {formatDate(prescription.prescribedDate)}</p>
                    </div>
                    <StatusBadge>{prescription.status}</StatusBadge>
                  </div>
                  <ul className="mt-4 space-y-2">
                    {(prescription.items || []).map((item) => (
                      <li className="rounded-lg bg-slate-50 p-3 text-sm" key={item.id}>
                        <p className="font-medium text-slate-900">{data.medications[item.medicationId]?.name || `Medication #${item.medicationId}`}</p>
                        <p className="mt-1 text-slate-600">{[item.dosage, item.frequency, item.duration, item.route].filter(Boolean).join(' · ')}</p>
                      </li>
                    ))}
                  </ul>
                  {prescription.instructions && <p className="mt-3 text-sm text-slate-600">{prescription.instructions}</p>}
                </article>
              ))}
            </div>
          ) : <EmptyState>No prescriptions are available.</EmptyState>}
        </Panel>
      )}
    </DoctorPage>
  );
}
