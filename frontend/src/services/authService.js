import apiClient, { TOKEN_STORAGE_KEY } from './apiClient.js';

const authService = {
  async login(email, password) {
    const { data } = await apiClient.post('/auth/login', { email, password });
    window.localStorage.setItem(TOKEN_STORAGE_KEY, data.token);
    return data;
  },

  async getCurrentUser() {
    const { data } = await apiClient.get('/users/me');
    return data;
  },

  getToken() {
    return window.localStorage.getItem(TOKEN_STORAGE_KEY);
  },

  clearSession() {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  },
};

export default authService;
