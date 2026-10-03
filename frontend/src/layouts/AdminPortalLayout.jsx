import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

const navigation = [
  ['dashboard', 'Dashboard', '⌂'],
  ['users', 'Users', '♙'],
  ['doctors', 'Doctors', '✚'],
  ['patients', 'Patients', '◉'],
  ['departments', 'Departments', '▦'],
  ['appointments', 'Appointments', '▣'],
];

export default function AdminPortalLayout() {
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const displayName = user?.email || 'Administrator';

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      {menuOpen && (
        <button
          aria-label="Close navigation menu"
          className="fixed inset-0 z-30 bg-slate-950/40 lg:hidden"
          onClick={() => setMenuOpen(false)}
          type="button"
        />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-slate-200 bg-white transition-transform lg:translate-x-0 ${
          menuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-20 items-center gap-3 border-b border-slate-100 px-6">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-slate-900 text-lg font-bold text-white">C</span>
          <div>
            <p className="font-semibold text-slate-950">CareConnect</p>
            <p className="text-xs text-slate-500">Administration</p>
          </div>
        </div>
        <nav aria-label="Admin navigation" className="flex-1 space-y-1 overflow-y-auto px-3 py-5">
          {navigation.map(([path, label, icon]) => (
            <NavLink
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                  isActive ? 'bg-slate-100 text-slate-950' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950'
                }`
              }
              key={path}
              onClick={() => setMenuOpen(false)}
              to={`/admin/${path}`}
            >
              <span aria-hidden="true" className="grid h-6 w-6 place-items-center text-base">{icon}</span>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-100 p-4">
          <div className="flex items-center gap-3 rounded-xl bg-slate-50 p-3">
            <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-slate-200 text-sm font-semibold text-slate-800">
              {displayName.slice(0, 1).toUpperCase()}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-slate-900">{displayName}</p>
              <p className="truncate text-xs text-slate-500">{user?.role}</p>
            </div>
          </div>
          <button
            className="mt-3 w-full rounded-xl px-3 py-2 text-left text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-950"
            onClick={handleLogout}
            type="button"
          >
            Sign out
          </button>
        </div>
      </aside>
      <div className="min-h-screen lg:pl-72">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white/95 px-4 backdrop-blur sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button
              aria-expanded={menuOpen}
              aria-label="Open navigation menu"
              className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
              onClick={() => setMenuOpen(true)}
              type="button"
            >
              <span aria-hidden="true">☰</span>
            </button>
            <p className="text-sm font-medium text-slate-600">Administration workspace</p>
          </div>
          <p className="hidden text-sm text-slate-500 sm:block">{displayName}</p>
        </header>
        <Outlet />
      </div>
    </div>
  );
}
