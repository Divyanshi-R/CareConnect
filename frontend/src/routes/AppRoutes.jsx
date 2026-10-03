import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute.jsx';
import PatientPortalLayout from '../layouts/PatientPortalLayout.jsx';
import LoginPage from '../pages/LoginPage.jsx';
import PatientAppointmentsPage from '../pages/patient/PatientAppointmentsPage.jsx';
import PatientDashboardPage from '../pages/patient/PatientDashboardPage.jsx';
import PatientMedicationsPage from '../pages/patient/PatientMedicationsPage.jsx';
import PatientMedicalRecordsPage from '../pages/patient/PatientMedicalRecordsPage.jsx';
import PatientOrdersPage from '../pages/patient/PatientOrdersPage.jsx';
import PatientPrescriptionsPage from '../pages/patient/PatientPrescriptionsPage.jsx';
import PatientProfilePage from '../pages/patient/PatientProfilePage.jsx';
import UnauthorizedPage from '../pages/UnauthorizedPage.jsx';

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/patient"
        element={
          <ProtectedRoute allowedRoles={['PATIENT']}>
            <PatientPortalLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/patient/dashboard" replace />} />
        <Route path="dashboard" element={<PatientDashboardPage />} />
        <Route path="profile" element={<PatientProfilePage />} />
        <Route path="appointments" element={<PatientAppointmentsPage />} />
        <Route path="medical-records" element={<PatientMedicalRecordsPage />} />
        <Route path="prescriptions" element={<PatientPrescriptionsPage />} />
        <Route path="medications" element={<PatientMedicationsPage />} />
        <Route path="orders" element={<PatientOrdersPage />} />
        <Route path="*" element={<Navigate to="/patient/dashboard" replace />} />
      </Route>
      <Route
        path="/doctor/*"
        element={
          <ProtectedRoute allowedRoles={['DOCTOR']}>
            <UnauthorizedPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <UnauthorizedPage />
          </ProtectedRoute>
        }
      />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route path="/" element={<Navigate to="/patient/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
