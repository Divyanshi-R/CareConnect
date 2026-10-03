import { useCallback, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useDoctorPortal } from '../../context/DoctorPortalContext.jsx';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import doctorService from '../../services/doctorService.js';
import {
  displayValue,
  DoctorPage,
  EmptyState,
  ErrorState,
  formatDate,
  LoadingState,
  Panel,
  patientName,
  StatusBadge,
} from '../../components/doctor/DoctorPageUi.jsx';

const tabs = ['Overview', 'Medical History', 'Encounters', 'Diagnoses', 'Medications', 'Prescriptions', 'Orders'];

function DetailField({ label, value }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-1 text-sm text-slate-900">{displayValue(value)}</dd>
    </div>
  );
}

export default function DoctorPatientDetailsPage() {
  const { id } = useParams();
  const { doctor } = useDoctorPortal();
  const [activeTab, setActiveTab] = useState(tabs[0]);
  const loadChart = useCallback(async () => {
    const patientId = Number(id);
    if (!Number.isSafeInteger(patientId) || patientId <= 0) {
      throw new Error('The patient ID is invalid.');
    }
    const [patient, encounters, diagnoses, allPrescriptions, allOrders, appointments, medications] = await Promise.all([
      doctorService.getPatient(patientId),
      doctorService.getPatientEncounters(patientId),
      doctorService.getPatientDiagnoses(patientId),
      doctorService.getPrescriptions(doctor.id),
      doctorService.getOrders(doctor.id),
      doctorService.getAppointments(doctor.id),
      doctorService.getMedications(),
    ]);
    const notes = await Promise.all(encounters.map((encounter) => doctorService.getEncounterNotes(encounter.id)));
    const prescriptions = allPrescriptions.filter((prescription) => prescription.patientId === patientId);
    return {
      patient,
      encounters,
      diagnoses,
      prescriptions,
      orders: allOrders.filter((order) => order.patientId === patientId),
      appointments: appointments.filter((appointment) => appointment.patientId === patientId),
      medications,
      notes: notes.flat(),
    };
  }, [doctor.id, id]);
  const { data, isLoading, error, reload } = useAsyncResource(loadChart);

  return (
    <DoctorPage
      title={data ? patientName(data.patient) : 'Patient chart'}
      description={data ? `Patient #${data.patient.id} · Date of birth ${formatDate(data.patient.dateOfBirth)}` : 'Loading the selected patient chart.'}
    >
      {isLoading ? <LoadingState label="Loading patient chart..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="space-y-5">
          <div className="flex flex-wrap gap-2 border-b border-slate-200 pb-3" role="tablist" aria-label="Patient chart sections">
            {tabs.map((tab) => (
              <button
                aria-selected={activeTab === tab}
                className={`rounded-lg px-3 py-2 text-sm font-medium ${activeTab === tab ? 'bg-indigo-700 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
                key={tab}
                onClick={() => setActiveTab(tab)}
                role="tab"
                type="button"
              >
                {tab}
              </button>
            ))}
          </div>

          {activeTab === 'Overview' && (
            <div className="grid gap-5 lg:grid-cols-2">
              <Panel title="Personal information">
                <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
                  <DetailField label="Patient ID" value={`#${data.patient.id}`} />
                  <DetailField label="Date of birth" value={formatDate(data.patient.dateOfBirth)} />
                  <DetailField label="Gender" value={data.patient.gender} />
                  <DetailField label="Blood group" value={data.patient.bloodGroup} />
                </dl>
              </Panel>
              <Panel title="Contact information">
                <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
                  <DetailField label="Phone" value={data.patient.phone} />
                  <DetailField label="Address" value={data.patient.address} />
                  <DetailField label="Emergency contact" value={data.patient.emergencyContact} />
                </dl>
              </Panel>
              <Panel title="Appointments" className="lg:col-span-2">
                {data.appointments.length ? (
                  <ul className="divide-y divide-slate-100">
                    {data.appointments.map((appointment) => (
                      <li className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0" key={appointment.id}>
                        <div>
                          <p className="font-medium text-slate-900">{formatDate(appointment.appointmentDate)} · {appointment.appointmentTime}</p>
                          <p className="mt-1 text-sm text-slate-500">{appointment.reason || 'Appointment'}</p>
                        </div>
                        <div className="flex items-center gap-3">
                          <StatusBadge>{appointment.status}</StatusBadge>
                          {data.encounters.some((encounter) => encounter.appointmentId === appointment.id) ? (
                            <Link
                              className="text-sm font-medium text-indigo-700"
                              to={`/doctor/encounters/${data.encounters.find((encounter) => encounter.appointmentId === appointment.id).id}`}
                            >
                              Open encounter
                            </Link>
                          ) : (
                            <Link className="text-sm font-medium text-indigo-700" to={`/doctor/encounters/new?appointmentId=${appointment.id}`}>
                              Start encounter
                            </Link>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : <EmptyState>No appointments with this doctor are available.</EmptyState>}
              </Panel>
            </div>
          )}
          {activeTab === 'Medical History' && (
            <Panel title="Medical history">
              {data.encounters.length ? data.encounters.map((encounter) => (
                <article className="border-b border-slate-100 py-4 last:border-0" key={encounter.id}>
                  <Link className="font-medium text-indigo-700" to={`/doctor/encounters/${encounter.id}`}>
                    {formatDate(encounter.encounterDate)} · {encounter.chiefComplaint}
                  </Link>
                  <p className="mt-2 text-sm text-slate-700"><strong>Diagnosis:</strong> {encounter.diagnosis || 'Not recorded'}</p>
                  <p className="mt-1 text-sm text-slate-700"><strong>Treatment:</strong> {encounter.treatmentPlan || 'Not recorded'}</p>
                  {data.notes.filter((note) => note.encounterId === encounter.id).map((note) => (
                    <p className="mt-2 whitespace-pre-wrap text-sm text-slate-600" key={note.id}>{note.content}</p>
                  ))}
                </article>
              )) : <EmptyState>No medical history is available for your encounters.</EmptyState>}
            </Panel>
          )}
          {activeTab === 'Encounters' && (
            <Panel title="Encounters">
              {data.encounters.length ? data.encounters.map((encounter) => (
                <article className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 py-3 last:border-0" key={encounter.id}>
                  <div><p className="font-medium text-slate-900">{encounter.chiefComplaint}</p><p className="mt-1 text-sm text-slate-500">{formatDate(encounter.encounterDate)}</p></div>
                  <Link className="text-sm font-medium text-indigo-700" to={`/doctor/encounters/${encounter.id}`}>Open encounter</Link>
                </article>
              )) : <EmptyState>No encounters are available.</EmptyState>}
            </Panel>
          )}
          {activeTab === 'Diagnoses' && (
            <Panel title="Diagnoses">
              {data.diagnoses.length ? data.diagnoses.map((diagnosis) => (
                <article className="border-b border-slate-100 py-3 last:border-0" key={diagnosis.id}>
                  <p className="font-medium text-slate-900">{diagnosis.diagnosisName} <span className="text-sm font-normal text-slate-500">({diagnosis.diagnosisCode})</span></p>
                  <p className="mt-1 text-sm text-slate-600">{diagnosis.description}</p>
                  <p className="mt-2"><StatusBadge>{diagnosis.status}</StatusBadge></p>
                </article>
              )) : <EmptyState>No diagnoses recorded by this doctor.</EmptyState>}
            </Panel>
          )}
          {activeTab === 'Medications' && (
            <Panel title="Medications from your prescriptions">
              {data.prescriptions.some((prescription) => prescription.status === 'ACTIVE' && prescription.items?.length) ? (
                <ul className="grid gap-3 md:grid-cols-2">
                  {data.prescriptions.filter((prescription) => prescription.status === 'ACTIVE').flatMap((prescription) =>
                    (prescription.items || []).map((item) => {
                      const medication = data.medications.find((candidate) => candidate.id === item.medicationId);
                      return (
                        <li className="rounded-xl border border-slate-100 p-4" key={`${prescription.id}-${item.id}`}>
                          <p className="font-medium text-slate-900">{medication?.name || `Medication #${item.medicationId}`}</p>
                          <p className="mt-1 text-sm text-slate-600">{[item.dosage, item.frequency, item.duration, item.route].filter(Boolean).join(' · ')}</p>
                          <p className="mt-1 text-xs text-slate-500">Prescribed {formatDate(prescription.prescribedDate)}</p>
                        </li>
                      );
                    }),
                  )}
                </ul>
              ) : <EmptyState>No active medications in your prescriptions.</EmptyState>}
            </Panel>
          )}
          {activeTab === 'Prescriptions' && (
            <Panel title="Prescriptions">
              {data.prescriptions.length ? data.prescriptions.map((prescription) => (
                <article className="border-b border-slate-100 py-3 last:border-0" key={prescription.id}>
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <p className="font-medium text-slate-900">Prescribed {formatDate(prescription.prescribedDate)}</p>
                    <StatusBadge>{prescription.status}</StatusBadge>
                  </div>
                  <p className="mt-1 text-sm text-slate-600">{(prescription.items || []).map((item) => {
                    const medication = data.medications.find((candidate) => candidate.id === item.medicationId);
                    return `${medication?.name || `Medication #${item.medicationId}`} — ${item.dosage}, ${item.frequency}, ${item.duration}`;
                  }).join('; ')}</p>
                </article>
              )) : <EmptyState>No prescriptions from this doctor.</EmptyState>}
            </Panel>
          )}
          {activeTab === 'Orders' && (
            <Panel title="Clinical orders">
              {data.orders.length ? data.orders.map((order) => (
                <article className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 py-3 last:border-0" key={order.id}>
                  <div><p className="font-medium text-slate-900">{order.orderType}: {order.description}</p><p className="mt-1 text-sm text-slate-500">{formatDate(order.orderedAt)}</p></div>
                  <StatusBadge>{order.status}</StatusBadge>
                </article>
              )) : <EmptyState>No clinical orders from this doctor.</EmptyState>}
            </Panel>
          )}
        </div>
      )}
    </DoctorPage>
  );
}
