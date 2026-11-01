import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import AuthLayout from '../components/AuthLayout.jsx';
import FormField from '../components/FormField.jsx';
import Notice from '../components/Notice.jsx';
import { isValidPassword, PASSWORD_HINT } from '../utils/password.js';

export default function Login() {
  const { user, status, login } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [email, setEmail] = useState(location.state?.email || '');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  // Only known internal destinations are accepted; never redirect to an arbitrary URL.
  const destination = ['/cart', '/#collection'].includes(location.state?.from) ? location.state.from : '/';

  if (status === 'loading') return <div className="route-state container" role="status">Đang kiểm tra phiên đăng nhập...</div>;
  if (user) return <Navigate to={destination} replace />;

  async function submit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) nextErrors.email = 'Vui lòng nhập email hợp lệ.';
    if (!isValidPassword(password)) nextErrors.password = 'Mật khẩu cần có từ 8 đến 15 ký tự.';
    setErrors(nextErrors);
    setError('');
    if (Object.keys(nextErrors).length) return;
    setBusy(true);
    try {
      await login(email.trim(), password);
      setPassword('');
      navigate(destination, { replace: true });
    } catch (failure) { setError(errorMessage(failure)); }
    finally { setBusy(false); }
  }

  return (
    <AuthLayout eyebrow="WELCOME BACK" title="Chào bạn trở lại." description="Đăng nhập để lưu những lựa chọn vào giỏ hàng của bạn.">
      {location.state?.notice && <Notice variant="success">{location.state.notice}</Notice>}
      {error && <Notice>{error}</Notice>}
      <form onSubmit={submit} noValidate className="account-form" aria-busy={busy}>
        <FormField id="login-email" label="Email" type="email" autoComplete="username" value={email} maxLength={150} onChange={(event) => setEmail(event.target.value)} error={errors.email} disabled={busy} required />
        <FormField id="login-password" label="Mật khẩu" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} error={errors.password} hint={PASSWORD_HINT} disabled={busy} required />
        <button type="submit" className="button button-dark account-submit" disabled={busy}>{busy ? 'Đang đăng nhập...' : 'Đăng nhập'}</button>
      </form>
      <p className="account-switch">Chưa có tài khoản? <Link to="/register" state={{ from: destination }}>Đăng ký</Link></p>
    </AuthLayout>
  );
}
