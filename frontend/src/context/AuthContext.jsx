import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { ApiError, apiRequest, errorMessage } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [csrfToken, setCsrfToken] = useState(null);
  const [status, setStatus] = useState('loading');
  const [authError, setAuthError] = useState('');
  const [notice, setNotice] = useState('');
  // Ignore responses that belong to an older login/logout/session check.
  const version = useRef(0);

  const refreshSession = useCallback(async (signal) => {
    const currentVersion = ++version.current;
    setStatus('loading');
    setAuthError('');
    try {
      const result = await apiRequest('/api/auth/me', { signal });
      if (!result.csrfToken) throw new ApiError(502, 'INVALID_RESPONSE');
      if (version.current === currentVersion && !signal?.aborted) {
        setUser(result.data);
        setCsrfToken(result.csrfToken);
        setStatus('ready');
      }
    } catch (error) {
      if (error.name === 'AbortError' || version.current !== currentVersion) return;
      if (error.status === 401) {
        setUser(null);
        setCsrfToken(null);
        setStatus('ready');
      } else {
        setAuthError(errorMessage(error));
        setStatus('error');
      }
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    refreshSession(controller.signal);
    return () => controller.abort();
  }, [refreshSession]);

  async function login(email, password) {
    const currentVersion = ++version.current;
    const result = await apiRequest('/api/auth/login', { method: 'POST', body: { email, password } });
    if (!result.csrfToken) throw new ApiError(502, 'INVALID_RESPONSE');
    if (version.current === currentVersion) {
      setUser(result.data);
      setCsrfToken(result.csrfToken);
      setStatus('ready');
      setAuthError('');
      setNotice('');
    }
    return result.data;
  }

  async function register({ fullName, email, password, phone }) {
    await apiRequest('/api/auth/register', { method: 'POST', body: { fullName, email, password, phone } });
    // Registration does not create a session in the existing Java API.
  }

  const request = useCallback(async (path, options = {}) => {
    const currentVersion = version.current;
    try {
      const result = await apiRequest(path, { ...options, csrfToken });
      if (version.current !== currentVersion) throw new ApiError(409, 'SESSION_CHANGED');
      return result.data;
    } catch (error) {
      if (version.current === currentVersion) {
        if (error.status === 401) {
          version.current++;
          setUser(null);
          setCsrfToken(null);
          setStatus('ready');
          setNotice('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.');
        } else if (error.code === 'CSRF_REJECTED') {
          // Recover the current token, but never replay a write automatically.
          setNotice('Phiên bảo mật đã thay đổi. Vui lòng kiểm tra giỏ hàng rồi thử lại thao tác.');
          await refreshSession();
        }
      }
      throw error;
    }
  }, [csrfToken, refreshSession]);

  async function logout() {
    await request('/api/auth/logout', { method: 'POST' });
    version.current++;
    setUser(null);
    setCsrfToken(null);
    setStatus('ready');
    setAuthError('');
    setNotice('');
  }

  return (
    <AuthContext.Provider value={{ user, status, authError, notice, login, register, logout, request, refreshSession, clearNotice: () => setNotice('') }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider.');
  return context;
}
