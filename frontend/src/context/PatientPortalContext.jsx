import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import patientService from '../services/patientService.js';

const PatientPortalContext = createContext(null);

export function PatientPortalProvider({ children }) {
  const [patient, setPatient] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const loadPatient = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      setPatient(await patientService.getMyProfile());
    } catch (requestError) {
      setPatient(null);
      setError(
        requestError.response?.data?.message ||
          'Unable to load your patient profile. Please try again.',
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPatient();
  }, [loadPatient]);

  const value = useMemo(
    () => ({ patient, isLoading, error, reloadPatient: loadPatient }),
    [patient, isLoading, error, loadPatient],
  );

  return <PatientPortalContext.Provider value={value}>{children}</PatientPortalContext.Provider>;
}

export function usePatientPortal() {
  const context = useContext(PatientPortalContext);
  if (!context) {
    throw new Error('usePatientPortal must be used within PatientPortalProvider');
  }
  return context;
}
