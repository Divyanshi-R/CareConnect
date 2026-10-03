import { useCallback } from 'react';
import adminService from '../../services/adminService.js';
import useAsyncResource from '../../hooks/useAsyncResource.js';
import {
  AdminPage,
  EmptyState,
  ErrorState,
  formatDate,
  LoadingState,
  Panel,
  StatusBadge,
} from '../../components/admin/AdminPageUi.jsx';

export default function AdminUsersPage() {
  const loadUsers = useCallback(() => adminService.getUsers(), []);
  const { data, isLoading, error, reload } = useAsyncResource(loadUsers);

  return (
    <AdminPage title="Users" description="User account directory. Passwords and authentication credentials are never returned or displayed.">
      {isLoading ? <LoadingState label="Loading users..." /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <Panel title={`${data.length} account${data.length === 1 ? '' : 's'}`}>
          {data.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px] text-left text-sm">
                <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                  <tr><th className="px-3 py-3">User ID</th><th className="px-3 py-3">Email</th><th className="px-3 py-3">Role</th><th className="px-3 py-3">Status</th><th className="px-3 py-3">Created</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.map((user) => (
                    <tr key={user.id}>
                      <td className="px-3 py-4">#{user.id}</td>
                      <td className="px-3 py-4 font-medium text-slate-900">{user.email}</td>
                      <td className="px-3 py-4"><StatusBadge>{user.role}</StatusBadge></td>
                      <td className="px-3 py-4"><StatusBadge>{user.enabled ? 'ENABLED' : 'DISABLED'}</StatusBadge></td>
                      <td className="px-3 py-4">{formatDate(user.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState>No user accounts are available.</EmptyState>}
        </Panel>
      )}
    </AdminPage>
  );
}
