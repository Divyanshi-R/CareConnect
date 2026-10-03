import { useCallback, useState } from 'react';
import adminService from '../../services/adminService.js';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import {
  AdminPage,
  adminInputClass,
  ApiError,
  EmptyState,
  ErrorState,
  LoadingState,
  Panel,
} from '../../components/admin/AdminPageUi.jsx';

export default function AdminDepartmentsPage() {
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', description: '' });
  const [mutationError, setMutationError] = useState(null);
  const [success, setSuccess] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const loadDepartments = useCallback(() => adminService.getDepartments(), []);
  const { data, isLoading, error, reload } = useAsyncResource(loadDepartments);

  function resetForm() {
    setEditing(null);
    setForm({ name: '', description: '' });
    setMutationError(null);
    setSuccess('');
  }

  function beginEdit(department) {
    setEditing(department);
    setForm({ name: department.name || '', description: department.description || '' });
    setMutationError(null);
    setSuccess('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setIsSaving(true);
    setMutationError(null);
    setSuccess('');
    try {
      if (editing) {
        await adminService.updateDepartment(editing.id, form);
        setSuccess('Department updated.');
      } else {
        await adminService.createDepartment(form);
        setSuccess('Department created.');
      }
      resetForm();
      setSuccess(editing ? 'Department updated.' : 'Department created.');
      reload();
    } catch (requestError) {
      setMutationError(requestError);
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(department) {
    if (!window.confirm(`Delete the ${department.name} department?`)) return;
    setMutationError(null);
    setSuccess('');
    try {
      await adminService.deleteDepartment(department.id);
      setSuccess('Department deleted.');
      reload();
    } catch (requestError) {
      setMutationError(requestError);
    }
  }

  return (
    <AdminPage title="Departments" description="Manage the departments available to CareConnect doctors.">
      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.3fr)]">
        <Panel title={editing ? `Edit ${editing.name}` : 'Create department'}>
          <form className="space-y-4" onSubmit={handleSubmit}>
            <label className="block text-sm font-medium text-slate-700" htmlFor="department-name">
              Name
              <input
                className={`${adminInputClass} mt-1`}
                id="department-name"
                maxLength={200}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                required
                value={form.name}
              />
            </label>
            <label className="block text-sm font-medium text-slate-700" htmlFor="department-description">
              Description
              <textarea
                className={`${adminInputClass} mt-1`}
                id="department-description"
                maxLength={1000}
                onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                rows={4}
                value={form.description}
              />
            </label>
            <ApiError error={mutationError} />
            {success && <p className="text-sm text-emerald-700" role="status">{success}</p>}
            <div className="flex flex-wrap gap-2">
              <button className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-60" disabled={isSaving} type="submit">
                {isSaving ? 'Saving...' : editing ? 'Save changes' : 'Create department'}
              </button>
              {editing && <button className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" onClick={resetForm} type="button">Cancel</button>}
            </div>
          </form>
        </Panel>

        {isLoading ? <Panel title="Departments"><LoadingState label="Loading departments..." /></Panel> : error ? (
          <Panel title="Departments"><ErrorState message={error} onRetry={reload} /></Panel>
        ) : (
          <Panel title={`${data.length} department${data.length === 1 ? '' : 's'}`}>
            {data.length ? (
              <div className="divide-y divide-slate-100">
                {data.map((department) => (
                  <article className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between" key={department.id}>
                    <div>
                      <h2 className="font-semibold text-slate-900">{department.name}</h2>
                      <p className="mt-1 whitespace-pre-wrap text-sm text-slate-600">{department.description || 'No description provided.'}</p>
                      <p className="mt-2 text-xs text-slate-500">Department #{department.id}</p>
                    </div>
                    <div className="flex shrink-0 gap-2">
                      <button className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-50" onClick={() => beginEdit(department)} type="button">Edit</button>
                      <button className="rounded-lg border border-red-200 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50" onClick={() => handleDelete(department)} type="button">Delete</button>
                    </div>
                  </article>
                ))}
              </div>
            ) : <EmptyState>No departments are available. Add one using the form.</EmptyState>}
          </Panel>
        )}
      </div>
    </AdminPage>
  );
}
