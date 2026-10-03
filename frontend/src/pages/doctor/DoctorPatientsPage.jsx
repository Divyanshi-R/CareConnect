import { useCallback, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
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

export default function DoctorPatientsPage() {
  const [search, setSearch] = useState('');
  const loadPatients = useCallback(() => doctorService.getPatients(), []);
  const { data, isLoading, error, reload } = useAsyncResource(loadPatients);
  const filteredPatients = useMemo(() => {
    if (!data) return [];
    const query = search.trim().toLocaleLowerCase();
    if (!query) return data;
    return data.filter((patient) =>
      [
        patient.firstName,
        patient.lastName,
        patient.id,
        patient.phone,
        patient.address,
      ].some((value) => String(value || '').toLocaleLowerCase().includes(query)),
    );
  }, [data, search]);

  return (
    <DoctorPage title="Patients" description="Search the patient directory and open a patient chart.">
      {isLoading ? <LoadingState label="Loading patients..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title={`${filteredPatients.length} patient${filteredPatients.length === 1 ? '' : 's'}`}>
          <input
            className="mb-5 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 sm:max-w-md"
            id="patient-search"
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search name, patient ID, or contact"
            type="search"
            value={search}
          />
          {filteredPatients.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[650px] text-left text-sm">
                <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                  <tr><th className="px-3 py-3">Patient</th><th className="px-3 py-3">Patient ID</th><th className="px-3 py-3">Date of birth</th><th className="px-3 py-3">Contact</th><th className="px-3 py-3">Chart</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredPatients.map((patient) => (
                    <tr key={patient.id}>
                      <td className="px-3 py-4 font-medium text-slate-900">{patientName(patient)}</td>
                      <td className="px-3 py-4">#{patient.id}</td>
                      <td className="px-3 py-4">{formatDate(patient.dateOfBirth)}</td>
                      <td className="px-3 py-4">{patient.phone || 'Not on file'}</td>
                      <td className="px-3 py-4">
                        <Link className="font-medium text-indigo-700 hover:text-indigo-900" to={`/doctor/patients/${patient.id}`}>
                          View chart
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState>{data.length ? 'No patients match your search.' : 'No patients are available.'}</EmptyState>}
        </Panel>
      )}
    </DoctorPage>
  );
}
