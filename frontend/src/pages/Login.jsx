import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import AuthLayout from '../components/AuthLayout.jsx';
import FormField from '../components/FormField.jsx';
import Notice from '../components/Notice.jsx';
import { isValidPassword, PASSWORD_HINT } from '../utils/password.js';
import { loginDestination } from '../utils/navigation.js';

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
  const destination = loginDestination(location.state?.from);

  if (status === 'loading') return <div className="route-state container" role="status">Checking your session...</div>;
  if (user) return <Navigate to={destination} replace />;

  async function submit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) nextErrors.email = 'Please enter a valid email address.';
    if (!isValidPassword(password)) nextErrors.password = 'Your password must be 8 to 15 characters long.';
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
    <AuthLayout eyebrow="WELCOME BACK" title="Welcome back." description="Log in to keep your favorite timepieces in your shopping cart.">
      {location.state?.notice && <Notice variant="success">{location.state.notice}</Notice>}
      {error && <Notice>{error}</Notice>}
      <form onSubmit={submit} noValidate className="account-form" aria-busy={busy}>
        <FormField id="login-email" label="Email" type="email" autoComplete="username" value={email} maxLength={150} onChange={(event) => setEmail(event.target.value)} error={errors.email} disabled={busy} required />
        <FormField id="login-password" label="Password" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} error={errors.password} hint={PASSWORD_HINT} disabled={busy} required />
        <button type="submit" className="button button-dark account-submit" disabled={busy}>{busy ? 'Logging in...' : 'Log In'}</button>
      </form>
      <p className="account-switch">New to HORA? <Link to="/register" state={{ from: destination }}>Register</Link></p>
    </AuthLayout>
  );
}
