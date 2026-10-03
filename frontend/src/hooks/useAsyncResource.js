import { useCallback, useEffect, useState } from 'react';

export default function useAsyncResource(loader) {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [requestKey, setRequestKey] = useState(0);

  const reload = useCallback(() => {
    setRequestKey((key) => key + 1);
  }, []);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError('');

    loader()
      .then((result) => {
        if (active) {
          setData(result);
        }
      })
      .catch((requestError) => {
        if (active) {
          setData(null);
          setError(
            requestError.response?.data?.message ||
              'Unable to load this information. Please try again.',
          );
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [loader, requestKey]);

  return { data, isLoading, error, reload };
}
