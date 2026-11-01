import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import AuthLayout from '../components/AuthLayout.jsx';
import FormField from '../components/FormField.jsx';
import Notice from '../components/Notice.jsx';
import { isValidPassword, PASSWORD_HINT } from '../utils/password.js';

export default function Register() {
  const { user, status, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', confirmation: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  if (status === 'loading') return <div className="route-state container" role="status">Checking your session...</div>;
  if (user) return <Navigate to="/" replace />;

  function update(field, value) { setForm((current) => ({ ...current, [field]: value })); }

  async function submit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!form.fullName.trim() || [...form.fullName.trim()].length > 100) nextErrors.fullName = 'Your full name must be 1 to 100 characters long.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()) || form.email.trim().length > 150) nextErrors.email = 'Enter a valid email address of up to 150 characters.';
    if ([...form.phone].length > 20) nextErrors.phone = 'Your phone number must not exceed 20 characters.';
    if (!isValidPassword(form.password)) nextErrors.password = 'Your password must be 8 to 15 characters long.';
    if (form.password !== form.confirmation) nextErrors.confirmation = 'Your passwords do not match.';
    setErrors(nextErrors);
    setError('');
    if (Object.keys(nextErrors).length) return;
    setBusy(true);
    try {
      await register({ fullName: form.fullName.trim(), email: form.email.trim(), password: form.password, phone: form.phone.trim() || null });
      setForm({ fullName: '', email: '', phone: '', password: '', confirmation: '' });
      navigate('/login', { replace: true, state: { email: form.email.trim(), from: location.state?.from, notice: 'Registration successful. You can now log in.' } });
    } catch (failure) { setError(errorMessage(failure)); }
    finally { setBusy(false); }
  }

  return (
    <AuthLayout eyebrow="YOUR NEXT CHAPTER" title="Begin your own story." description="Create an account and find a timepiece to make your own.">
      {error && <Notice>{error}</Notice>}
      <form onSubmit={submit} noValidate className="account-form" aria-busy={busy}>
        <FormField id="register-name" label="Full Name" autoComplete="name" value={form.fullName} maxLength={100} onChange={(event) => update('fullName', event.target.value)} error={errors.fullName} disabled={busy} required />
        <FormField id="register-email" label="Email" type="email" autoComplete="username" value={form.email} maxLength={150} onChange={(event) => update('email', event.target.value)} error={errors.email} disabled={busy} required />
        <FormField id="register-phone" label="Phone Number (optional)" type="tel" autoComplete="tel" value={form.phone} maxLength={20} onChange={(event) => update('phone', event.target.value)} error={errors.phone} disabled={busy} />
        <FormField id="register-password" label="Password" type="password" autoComplete="new-password" value={form.password} onChange={(event) => update('password', event.target.value)} error={errors.password} hint={PASSWORD_HINT} disabled={busy} required />
        <FormField id="register-confirmation" label="Confirm Password" type="password" autoComplete="new-password" value={form.confirmation} onChange={(event) => update('confirmation', event.target.value)} error={errors.confirmation} disabled={busy} required />
        <button type="submit" className="button button-dark account-submit" disabled={busy}>{busy ? 'Creating account...' : 'Create Account'}</button>
      </form>
      <p className="account-switch">Already have an account? <Link to="/login" state={{ from: location.state?.from }}>Log In</Link></p>
    </AuthLayout>
  );
}
