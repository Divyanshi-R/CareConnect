import { Navigate, Route, Routes } from 'react-router-dom';
import AdminDashboardPage from '../pages/admin/AdminDashboardPage.jsx';
import AdminAppointmentsPage from '../pages/admin/AdminAppointmentsPage.jsx';
import AdminDepartmentsPage from '../pages/admin/AdminDepartmentsPage.jsx';
import AdminDoctorsPage from '../pages/admin/AdminDoctorsPage.jsx';
import AdminPatientsPage from '../pages/admin/AdminPatientsPage.jsx';
import AdminUsersPage from '../pages/admin/AdminUsersPage.jsx';
import AdminPortalLayout from '../layouts/AdminPortalLayout.jsx';
import ProtectedRoute from '../components/ProtectedRoute.jsx';
import DoctorPortalLayout from '../layouts/DoctorPortalLayout.jsx';
import PatientPortalLayout from '../layouts/PatientPortalLayout.jsx';
import DoctorAppointmentsPage from '../pages/doctor/DoctorAppointmentsPage.jsx';
import DoctorDashboardPage from '../pages/doctor/DoctorDashboardPage.jsx';
import DoctorEncounterPage from '../pages/doctor/DoctorEncounterPage.jsx';
import DoctorOrdersPage from '../pages/doctor/DoctorOrdersPage.jsx';
import DoctorPatientDetailsPage from '../pages/doctor/DoctorPatientDetailsPage.jsx';
import DoctorPatientsPage from '../pages/doctor/DoctorPatientsPage.jsx';
import DoctorPrescriptionsPage from '../pages/doctor/DoctorPrescriptionsPage.jsx';
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
            <DoctorPortalLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/doctor/dashboard" replace />} />
        <Route path="dashboard" element={<DoctorDashboardPage />} />
        <Route path="appointments" element={<DoctorAppointmentsPage />} />
        <Route path="patients" element={<DoctorPatientsPage />} />
        <Route path="patients/:id" element={<DoctorPatientDetailsPage />} />
        <Route path="encounters/:id" element={<DoctorEncounterPage />} />
        <Route path="prescriptions" element={<DoctorPrescriptionsPage />} />
        <Route path="orders" element={<DoctorOrdersPage />} />
        <Route path="*" element={<Navigate to="/doctor/dashboard" replace />} />
      </Route>
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminPortalLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/admin/dashboard" replace />} />
        <Route path="dashboard" element={<AdminDashboardPage />} />
        <Route path="users" element={<AdminUsersPage />} />
        <Route path="doctors" element={<AdminDoctorsPage />} />
        <Route path="patients" element={<AdminPatientsPage />} />
        <Route path="departments" element={<AdminDepartmentsPage />} />
        <Route path="appointments" element={<AdminAppointmentsPage />} />
        <Route path="*" element={<Navigate to="/admin/dashboard" replace />} />
      </Route>
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route path="/" element={<Navigate to="/patient/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
