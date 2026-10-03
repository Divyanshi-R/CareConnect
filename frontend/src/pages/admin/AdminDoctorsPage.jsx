import { useCallback } from 'react';
import adminService from '../../services/adminService.js';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import { AdminPage, EmptyState, ErrorState, LoadingState, Panel } from '../../components/admin/AdminPageUi.jsx';

export default function AdminDoctorsPage() {
  const loadDoctors = useCallback(async () => {
    const [doctors, departments] = await Promise.all([
      adminService.getDoctors(),
      adminService.getDepartments(),
    ]);
    return {
      doctors,
      departments: Object.fromEntries(departments.map((department) => [department.id, department])),
    };
  }, []);
  const { data, isLoading, error, reload } = useAsyncResource(loadDoctors);

  return (
    <AdminPage title="Doctors" description="Doctor profiles and their assigned departments.">
      {isLoading ? <LoadingState label="Loading doctors..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title={`${data.doctors.length} doctor${data.doctors.length === 1 ? '' : 's'}`}>
          {data.doctors.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                  <tr><th className="px-3 py-3">Doctor</th><th className="px-3 py-3">Doctor ID</th><th className="px-3 py-3">Specialization</th><th className="px-3 py-3">Department</th><th className="px-3 py-3">Contact</th><th className="px-3 py-3">License</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.doctors.map((doctor) => (
                    <tr key={doctor.id}>
                      <td className="px-3 py-4 font-medium text-slate-900">{[doctor.firstName, doctor.lastName].filter(Boolean).join(' ') || 'Unnamed doctor'}</td>
                      <td className="px-3 py-4">#{doctor.id}</td>
                      <td className="px-3 py-4">{doctor.specialization || 'Not recorded'}</td>
                      <td className="px-3 py-4">{data.departments[doctor.departmentId]?.name || 'Not assigned'}</td>
                      <td className="px-3 py-4">{doctor.phone || 'Not recorded'}</td>
                      <td className="px-3 py-4">{doctor.licenseNumber || 'Not recorded'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState>No doctor profiles are available.</EmptyState>}
        </Panel>
      )}
    </AdminPage>
  );
}
