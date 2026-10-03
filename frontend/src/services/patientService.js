import apiClient from './apiClient.js';

async function getList(path) {
  const { data } = await apiClient.get(path);
  if (!Array.isArray(data)) {
    throw new Error('The server returned an unexpected response.');
  }
  return data;
}

const patientService = {
  async getMyProfile() {
    const { data } = await apiClient.get('/patients/me');
    return data;
  },

  getAppointments(patientId) {
    return getList(`/appointments/patient/${patientId}`);
  },

  getEncounters(patientId) {
    return getList(`/encounters/patient/${patientId}`);
  },

  getDiagnoses(patientId) {
    return getList(`/patients/${patientId}/diagnoses`);
  },

  getMyNotes() {
    return getList('/patients/me/notes');
  },

  getPrescriptions(patientId) {
    return getList(`/patients/${patientId}/prescriptions`);
  },

  getMyMedications() {
    return getList('/patients/me/medications');
  },

  getOrders(patientId) {
    return getList(`/patients/${patientId}/orders`);
  },

  async getDoctor(doctorId) {
    const { data } = await apiClient.get(`/doctors/${doctorId}`);
    return data;
  },

  async getDoctors(records) {
    const doctorIds = [...new Set(records.map((record) => record.doctorId).filter(Boolean))];
    const doctors = await Promise.all(
      doctorIds.map(async (id) => [id, await patientService.getDoctor(id)]),
    );
    return Object.fromEntries(doctors);
  },
};

export default patientService;
