import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useAuth } from './AuthContext.jsx';
import doctorService from '../services/doctorService.js';

const DoctorPortalContext = createContext(null);

export function DoctorPortalProvider({ children }) {
  const { user } = useAuth();
  const [doctor, setDoctor] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDoctor = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      setDoctor(await doctorService.getMyProfile(user.id));
    } catch (requestError) {
      setDoctor(null);
      setError(requestError.response?.data?.message || requestError.message || 'Unable to load your doctor profile.');
    } finally {
      setIsLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    loadDoctor();
  }, [loadDoctor]);

  const value = useMemo(
    () => ({ doctor, isLoading, error, reloadDoctor: loadDoctor }),
    [doctor, isLoading, error, loadDoctor],
  );

  return <DoctorPortalContext.Provider value={value}>{children}</DoctorPortalContext.Provider>;
}

export function useDoctorPortal() {
  const context = useContext(DoctorPortalContext);
  if (!context) {
    throw new Error('useDoctorPortal must be used within DoctorPortalProvider');
  }
  return context;
}
