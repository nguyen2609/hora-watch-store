import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';

// A cancelled read cannot overwrite a newer route or retry.
export default function useAdminData(path) {
  const { request } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    setData(null);
    request(path, { signal: controller.signal })
      .then(result => { if (!controller.signal.aborted) setData(result); })
      .catch(failure => { if (!controller.signal.aborted) setError(errorMessage(failure, 'admin')); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [path, request, revision]);
  return { data, setData, loading, error, reload: () => setRevision(value => value + 1) };
}
