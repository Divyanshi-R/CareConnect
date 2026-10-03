import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import authService from '../services/authService.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionError, setSessionError] = useState('');

  const logout = useCallback(() => {
    authService.clearSession();
    setUser(null);
  }, []);

  const restoreSession = useCallback(async () => {
    setSessionError('');
    if (!authService.getToken()) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const currentUser = await authService.getCurrentUser();
      setUser(currentUser);
    } catch (error) {
      if (error.response?.status === 401) {
        authService.clearSession();
      } else {
        setSessionError('Unable to verify your session. Check your connection and try again.');
      }
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  const login = useCallback(async (email, password) => {
    await authService.login(email, password);
    try {
      const currentUser = await authService.getCurrentUser();
      setUser(currentUser);
      return currentUser;
    } catch (error) {
      authService.clearSession();
      setUser(null);
      throw error;
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      isLoading,
      sessionError,
      restoreSession,
      login,
      logout,
      isAuthenticated: Boolean(user),
    }),
    [user, isLoading, sessionError, restoreSession, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
