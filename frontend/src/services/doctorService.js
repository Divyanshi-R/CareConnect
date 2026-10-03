import apiClient from './apiClient.js';

async function getList(path) {
  const { data } = await apiClient.get(path);
  if (!Array.isArray(data)) {
    throw new Error('The server returned an unexpected response.');
  }
  return data;
}

const doctorService = {
  async getMyProfile(userId) {
    const doctors = await getList('/doctors');
    const doctor = doctors.find((item) => item.userId === userId);
    if (!doctor) {
      throw new Error('No doctor profile is associated with this account.');
    }
    return doctor;
  },

  getAppointments(doctorId) {
    return getList(`/appointments/doctor/${doctorId}`);
  },

  async getAppointment(appointmentId) {
    const { data } = await apiClient.get(`/appointments/${appointmentId}`);
    return data;
  },

  getPatients() {
    return getList('/patients');
  },

  async getPatient(patientId) {
    const { data } = await apiClient.get(`/patients/${patientId}`);
    return data;
  },

  getDoctorEncounters(doctorId) {
    return getList(`/encounters/doctor/${doctorId}`);
  },

  getPatientEncounters(patientId) {
    return getList(`/encounters/patient/${patientId}`);
  },

  async getEncounter(encounterId) {
    const { data } = await apiClient.get(`/encounters/${encounterId}`);
    return data;
  },

  getPatientDiagnoses(patientId) {
    return getList(`/patients/${patientId}/diagnoses`);
  },

  getEncounterDiagnoses(encounterId) {
    return getList(`/encounters/${encounterId}/diagnoses`);
  },

  getEncounterNotes(encounterId) {
    return getList(`/encounters/${encounterId}/notes`);
  },

  getPrescriptions(doctorId) {
    return getList(`/doctors/${doctorId}/prescriptions`);
  },

  getOrders(doctorId) {
    return getList(`/doctors/${doctorId}/orders`);
  },

  getMedications() {
    return getList('/medications');
  },

  async createEncounter(request) {
    const { data } = await apiClient.post('/encounters', request);
    return data;
  },

  async updateEncounter(encounterId, request) {
    const { data } = await apiClient.put(`/encounters/${encounterId}`, request);
    return data;
  },

  async createNote(encounterId, request) {
    const { data } = await apiClient.post(`/encounters/${encounterId}/notes`, request);
    return data;
  },

  async createDiagnosis(encounterId, request) {
    const { data } = await apiClient.post(`/encounters/${encounterId}/diagnoses`, request);
    return data;
  },

  async createPrescription(request) {
    const { data } = await apiClient.post('/prescriptions', request);
    return data;
  },

  async createOrder(request) {
    const { data } = await apiClient.post('/orders', request);
    return data;
  },
};

export default doctorService;
