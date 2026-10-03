import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function UnauthorizedPage() {
  const { logout } = useAuth();

  return (
    <main className="grid min-h-screen place-items-center px-4">
      <section className="max-w-md rounded-2xl bg-white p-8 text-center shadow-sm ring-1 ring-slate-200">
        <h1 className="text-2xl font-semibold text-slate-900">Access denied</h1>
        <p className="mt-3 text-slate-600">This account does not have access to that area.</p>
        <div className="mt-6 flex justify-center gap-4">
          <Link className="font-medium text-teal-700 hover:text-teal-800" to="/patient/dashboard">
            Patient portal
          </Link>
          <button className="font-medium text-slate-600 hover:text-slate-900" onClick={logout} type="button">
            Sign out
          </button>
        </div>
      </section>
    </main>
  );
}
