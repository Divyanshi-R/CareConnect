import { useState } from 'react';
import {
  ApiFormError,
  currentDateString,
  FormField,
  FormSelect,
  FormTextArea,
  SubmitButton,
} from '../../components/doctor/DoctorPageUi.jsx';
import doctorService from '../../services/doctorService.js';

function useFormRequest() {
  const [error, setError] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [success, setSuccess] = useState('');

  async function submit(request, onSaved) {
    setError(null);
    setSuccess('');
    setIsSaving(true);
    try {
      const result = await request();
      setSuccess('Saved successfully.');
      onSaved?.(result);
      return true;
    } catch (requestError) {
      setError(requestError);
      return false;
    } finally {
      setIsSaving(false);
    }
  }

  return { error, isSaving, success, submit };
}

function SectionForm({ title, children, error, success, isSaving, buttonText, onSubmit }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5">
      <h2 className="font-semibold text-slate-950">{title}</h2>
      <form className="mt-4 space-y-4" onSubmit={onSubmit}>
        {children}
        <ApiFormError error={error} />
        {success && <p className="text-sm text-emerald-700" role="status">{success}</p>}
        <SubmitButton disabled={isSaving}>{isSaving ? 'Saving...' : buttonText}</SubmitButton>
      </form>
    </section>
  );
}

export function StartEncounterForm({ appointment, patient, doctor, onCreated }) {
  const [form, setForm] = useState({
    chiefComplaint: appointment.reason || '',
    diagnosis: '',
    symptoms: '',
    treatmentPlan: '',
    vitals: '',
  });
  const request = useFormRequest();
  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }
  async function handleSubmit(event) {
    event.preventDefault();
    await request.submit(
      () => doctorService.createEncounter({
        patientId: patient.id,
        doctorId: doctor.id,
        appointmentId: appointment.id,
        encounterDate: currentDateString(),
        chiefComplaint: form.chiefComplaint.trim(),
        diagnosis: form.diagnosis.trim(),
        treatmentPlan: form.treatmentPlan.trim(),
        notes: form.symptoms.trim(),
        vitals: form.vitals.trim(),
      }),
      onCreated,
    );
  }

  return (
    <SectionForm
      buttonText="Create encounter"
      error={request.error}
      isSaving={request.isSaving}
      onSubmit={handleSubmit}
      success={request.success}
      title={`Start encounter for ${patient.firstName} ${patient.lastName}`}
    >
      <p className="text-sm text-amber-800">
        This backend endpoint creates a persisted encounter immediately. A diagnosis is required to create it.
      </p>
      <FormField label="Chief complaint" maxLength={1000} name="chiefComplaint" onChange={update} required value={form.chiefComplaint} />
      <FormTextArea label="Diagnosis" maxLength={2000} name="diagnosis" onChange={update} required value={form.diagnosis} />
      <FormTextArea label="Symptoms" maxLength={4000} name="symptoms" onChange={update} value={form.symptoms} />
      <FormTextArea label="Treatment plan" maxLength={2000} name="treatmentPlan" onChange={update} value={form.treatmentPlan} />
      <FormTextArea label="Vitals" maxLength={2000} name="vitals" onChange={update} value={form.vitals} />
    </SectionForm>
  );
}

export function EncounterEditor({ encounter, onSaved }) {
  const [form, setForm] = useState({
    encounterDate: encounter.encounterDate || '',
    chiefComplaint: encounter.chiefComplaint || '',
    diagnosis: encounter.diagnosis || '',
    treatmentPlan: encounter.treatmentPlan || '',
    symptoms: encounter.notes || '',
    vitals: encounter.vitals || '',
  });
  const request = useFormRequest();
  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }
  async function handleSubmit(event) {
    event.preventDefault();
    await request.submit(
      () => doctorService.updateEncounter(encounter.id, {
        patientId: encounter.patientId,
        doctorId: encounter.doctorId,
        appointmentId: encounter.appointmentId,
        encounterDate: form.encounterDate,
        chiefComplaint: form.chiefComplaint.trim(),
        diagnosis: form.diagnosis.trim(),
        treatmentPlan: form.treatmentPlan.trim(),
        notes: form.symptoms.trim(),
        vitals: form.vitals.trim(),
      }),
      onSaved,
    );
  }
  return (
    <SectionForm
      buttonText="Save encounter"
      error={request.error}
      isSaving={request.isSaving}
      onSubmit={handleSubmit}
      success={request.success}
      title="Encounter details"
    >
      <FormField label="Encounter date" name="encounterDate" onChange={update} required type="date" value={form.encounterDate} />
      <FormField label="Chief complaint" maxLength={1000} name="chiefComplaint" onChange={update} required value={form.chiefComplaint} />
      <FormTextArea label="Diagnosis" maxLength={2000} name="diagnosis" onChange={update} required value={form.diagnosis} />
      <FormTextArea label="Symptoms" maxLength={4000} name="symptoms" onChange={update} value={form.symptoms} />
      <FormTextArea label="Treatment plan" maxLength={2000} name="treatmentPlan" onChange={update} value={form.treatmentPlan} />
      <FormTextArea label="Vitals" maxLength={2000} name="vitals" onChange={update} value={form.vitals} />
    </SectionForm>
  );
}

export function EncounterActionForms({ encounter, medications, onSaved }) {
  const [note, setNote] = useState({ noteType: 'GENERAL', content: '' });
  const [diagnosis, setDiagnosis] = useState({
    diagnosisName: '',
    description: '',
    diagnosisCode: '',
    status: 'ACTIVE',
  });
  const [prescription, setPrescription] = useState({
    medicationId: '',
    dosage: '',
    frequency: '',
    duration: '',
    route: '',
    itemInstructions: '',
    instructions: '',
  });
  const [order, setOrder] = useState({ orderType: 'LAB', description: '', priority: 'ROUTINE' });
  const noteRequest = useFormRequest();
  const diagnosisRequest = useFormRequest();
  const prescriptionRequest = useFormRequest();
  const orderRequest = useFormRequest();

  function update(setter) {
    return (event) => setter((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function saveNote(event) {
    event.preventDefault();
    if (await noteRequest.submit(() => doctorService.createNote(encounter.id, note), onSaved)) {
      setNote((current) => ({ ...current, content: '' }));
    }
  }

  async function saveDiagnosis(event) {
    event.preventDefault();
    if (await diagnosisRequest.submit(
      () => doctorService.createDiagnosis(encounter.id, {
        patientId: encounter.patientId,
        doctorId: encounter.doctorId,
        diagnosisName: diagnosis.diagnosisName.trim(),
        description: diagnosis.description.trim(),
        diagnosisCode: diagnosis.diagnosisCode.trim(),
        diagnosedAt: new Date().toISOString().slice(0, 19),
        status: diagnosis.status,
      }),
      onSaved,
    )) {
      setDiagnosis({ diagnosisName: '', description: '', diagnosisCode: '', status: 'ACTIVE' });
    }
  }

  async function savePrescription(event) {
    event.preventDefault();
    if (await prescriptionRequest.submit(
      () => doctorService.createPrescription({
        patientId: encounter.patientId,
        doctorId: encounter.doctorId,
        encounterId: encounter.id,
        prescribedDate: currentDateString(),
        instructions: prescription.instructions.trim(),
        status: 'ACTIVE',
        items: [{
          medicationId: Number(prescription.medicationId),
          dosage: prescription.dosage.trim(),
          frequency: prescription.frequency.trim(),
          duration: prescription.duration.trim(),
          route: prescription.route.trim(),
          instructions: prescription.itemInstructions.trim(),
        }],
      }),
      onSaved,
    )) {
      setPrescription({ medicationId: '', dosage: '', frequency: '', duration: '', route: '', itemInstructions: '', instructions: '' });
    }
  }

  async function saveOrder(event) {
    event.preventDefault();
    if (await orderRequest.submit(
      () => doctorService.createOrder({
        patientId: encounter.patientId,
        doctorId: encounter.doctorId,
        encounterId: encounter.id,
        orderType: order.orderType,
        description: order.description.trim(),
        priority: order.priority,
        status: 'ORDERED',
      }),
      onSaved,
    )) {
      setOrder({ orderType: 'LAB', description: '', priority: 'ROUTINE' });
    }
  }

  const medicationOptions = [
    ['', 'Select medication'],
    ...medications.filter((medication) => medication.active).map((medication) => [
      String(medication.id),
      `${medication.name}${medication.strength ? ` · ${medication.strength}` : ''}`,
    ]),
  ];

  return (
    <div className="space-y-5">
      <SectionForm buttonText="Add clinical note" error={noteRequest.error} isSaving={noteRequest.isSaving} onSubmit={saveNote} success={noteRequest.success} title="Clinical note">
        <FormSelect label="Note type" name="noteType" onChange={update(setNote)} options={['GENERAL', 'PROGRESS', 'ASSESSMENT', 'TREATMENT_PLAN'].map((type) => [type, type.replaceAll('_', ' ')])} value={note.noteType} />
        <FormTextArea label="Note content" maxLength={4000} name="content" onChange={update(setNote)} required value={note.content} />
      </SectionForm>

      <SectionForm buttonText="Add diagnosis" error={diagnosisRequest.error} isSaving={diagnosisRequest.isSaving} onSubmit={saveDiagnosis} success={diagnosisRequest.success} title="Diagnosis">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Diagnosis name" maxLength={200} name="diagnosisName" onChange={update(setDiagnosis)} required value={diagnosis.diagnosisName} />
          <FormField label="Diagnosis code" maxLength={100} name="diagnosisCode" onChange={update(setDiagnosis)} required value={diagnosis.diagnosisCode} />
          <FormSelect label="Status" name="status" onChange={update(setDiagnosis)} options={['ACTIVE', 'RESOLVED', 'HISTORICAL'].map((status) => [status, status])} value={diagnosis.status} />
        </div>
        <FormTextArea label="Description" maxLength={4000} name="description" onChange={update(setDiagnosis)} required value={diagnosis.description} />
      </SectionForm>

      <SectionForm buttonText="Create prescription" error={prescriptionRequest.error} isSaving={prescriptionRequest.isSaving} onSubmit={savePrescription} success={prescriptionRequest.success} title="Prescription">
        <FormSelect label="Medication" name="medicationId" onChange={update(setPrescription)} options={medicationOptions} required value={prescription.medicationId} />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Dosage" maxLength={200} name="dosage" onChange={update(setPrescription)} required value={prescription.dosage} />
          <FormField label="Frequency" maxLength={200} name="frequency" onChange={update(setPrescription)} required value={prescription.frequency} />
          <FormField label="Duration" maxLength={200} name="duration" onChange={update(setPrescription)} required value={prescription.duration} />
          <FormField label="Route" maxLength={100} name="route" onChange={update(setPrescription)} required value={prescription.route} />
        </div>
        <FormTextArea label="Medication instructions" maxLength={2000} name="itemInstructions" onChange={update(setPrescription)} value={prescription.itemInstructions} />
        <FormTextArea label="Prescription instructions" maxLength={4000} name="instructions" onChange={update(setPrescription)} value={prescription.instructions} />
      </SectionForm>

      <SectionForm buttonText="Create clinical order" error={orderRequest.error} isSaving={orderRequest.isSaving} onSubmit={saveOrder} success={orderRequest.success} title="Clinical order">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormSelect label="Order type" name="orderType" onChange={update(setOrder)} options={['LAB', 'IMAGING', 'OTHER'].map((type) => [type, type])} value={order.orderType} />
          <FormSelect label="Priority" name="priority" onChange={update(setOrder)} options={['ROUTINE', 'URGENT'].map((priority) => [priority, priority])} value={order.priority} />
        </div>
        <FormTextArea label="Description" maxLength={4000} name="description" onChange={update(setOrder)} required value={order.description} />
      </SectionForm>
    </div>
  );
}
