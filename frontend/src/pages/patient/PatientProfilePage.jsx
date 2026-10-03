import { displayValue, Panel, PatientPage } from '../../components/patient/PatientPageUi.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import { usePatientPortal } from '../../context/PatientPortalContext.jsx';

function Field({ label, value }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-1 text-sm text-slate-900">{displayValue(value)}</dd>
    </div>
  );
}

export default function PatientProfilePage() {
  const { patient } = usePatientPortal();
  const { user } = useAuth();
  return (
    <PatientPage title="My profile" description="Review the personal information associated with your patient account.">
      <div className="grid gap-5 lg:grid-cols-2">
        <Panel title="Personal information">
          <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
            <Field label="First name" value={patient.firstName} />
            <Field label="Last name" value={patient.lastName} />
            <Field label="Date of birth" value={patient.dateOfBirth} />
            <Field label="Gender" value={patient.gender} />
            <Field label="Blood group" value={patient.bloodGroup} />
          </dl>
        </Panel>
        <Panel title="Contact information">
          <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
            <Field label="Email" value={user?.email} />
            <Field label="Phone" value={patient.phone} />
            <Field label="Address" value={patient.address} />
          </dl>
        </Panel>
        <Panel title="Emergency information" className="lg:col-span-2">
          <dl><Field label="Emergency contact" value={patient.emergencyContact} /></dl>
        </Panel>
      </div>
    </PatientPage>
  );
}
