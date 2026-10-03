import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function ProtectedRoute({ allowedRoles, children }) {
  const { user, isLoading, sessionError, restoreSession } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <main className="grid min-h-screen place-items-center" aria-live="polite">
        <p className="text-sm text-slate-600">Checking your session...</p>
      </main>
    );
  }

  if (sessionError) {
    return (
      <main className="grid min-h-screen place-items-center px-4">
        <section className="max-w-md text-center">
          <p className="text-slate-700" role="alert">{sessionError}</p>
          <button
            className="mt-4 rounded-lg bg-teal-700 px-4 py-2 font-medium text-white hover:bg-teal-800"
            onClick={restoreSession}
            type="button"
          >
            Retry
          </button>
        </section>
      </main>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}
