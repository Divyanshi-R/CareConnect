import apiClient from './apiClient.js';

async function getList(path) {
  const { data } = await apiClient.get(path);
  if (!Array.isArray(data)) {
    throw new Error('The server returned an unexpected response.');
  }
  return data;
}

const adminService = {
  getUsers() {
    return getList('/users');
  },

  getDoctors() {
    return getList('/doctors');
  },

  getPatients() {
    return getList('/patients');
  },

  getDepartments() {
    return getList('/departments');
  },

  getAppointments() {
    return getList('/appointments');
  },

  async createDepartment(request) {
    const { data } = await apiClient.post('/departments', request);
    return data;
  },

  async updateDepartment(id, request) {
    const { data } = await apiClient.put(`/departments/${id}`, request);
    return data;
  },

  async deleteDepartment(id) {
    await apiClient.delete(`/departments/${id}`);
  },

  async cancelAppointment(id) {
    await apiClient.delete(`/appointments/${id}`);
  },
};

export default adminService;
