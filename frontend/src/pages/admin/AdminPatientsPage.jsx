import { useCallback } from 'react';
import adminService from '../../services/adminService.js';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import { AdminPage, EmptyState, ErrorState, formatDate, LoadingState, Panel } from '../../components/admin/AdminPageUi.jsx';

export default function AdminPatientsPage() {
  const loadPatients = useCallback(() => adminService.getPatients(), []);
  const { data, isLoading, error, reload } = useAsyncResource(loadPatients);

  return (
    <AdminPage title="Patients" description="Patient directory and available profile information.">
      {isLoading ? <LoadingState label="Loading patients..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title={`${data.length} patient${data.length === 1 ? '' : 's'}`}>
          {data.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                  <tr><th className="px-3 py-3">Patient</th><th className="px-3 py-3">Patient ID</th><th className="px-3 py-3">Date of birth</th><th className="px-3 py-3">Gender</th><th className="px-3 py-3">Phone</th><th className="px-3 py-3">Blood group</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.map((patient) => (
                    <tr key={patient.id}>
                      <td className="px-3 py-4 font-medium text-slate-900">{[patient.firstName, patient.lastName].filter(Boolean).join(' ') || 'Unnamed patient'}</td>
                      <td className="px-3 py-4">#{patient.id}</td>
                      <td className="px-3 py-4">{formatDate(patient.dateOfBirth)}</td>
                      <td className="px-3 py-4">{patient.gender || 'Not recorded'}</td>
                      <td className="px-3 py-4">{patient.phone || 'Not recorded'}</td>
                      <td className="px-3 py-4">{patient.bloodGroup || 'Not recorded'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState>No patient profiles are available.</EmptyState>}
        </Panel>
      )}
    </AdminPage>
  );
}
