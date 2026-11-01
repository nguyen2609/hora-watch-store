import Icon from './Icon.jsx';
import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import Notice from './Notice.jsx';

export default function Navbar({ onTypeChange }) {
  const { user, status, logout } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function signOut() {
    setBusy(true);
    setError('');
    try { await logout(); navigate('/', { replace: true }); }
    catch (failure) { if (failure.status !== 401) setError(errorMessage(failure)); }
    finally { setBusy(false); }
  }
  return (
    <>
      <div className="announcement">YOUR SIGNATURE, IN EVERY MOMENT</div>
      <header className="site-header">
        <nav className="navbar container" aria-label="Main navigation">
          <Link className="brand" to="/" aria-label="HORA — Home">
            <span className="brand-mark"><Icon name="clock" size={28} /></span>
            <span>HORA<span className="brand-caption">WATCH STORE</span></span>
          </Link>
          <div className="nav-links">
            <Link to="/#collection" onClick={() => onTypeChange('ALL')}>Products</Link>
            <Link to="/#collection" onClick={() => onTypeChange('AUTOMATIC')}>Automatic</Link>
            <Link to="/#collection" onClick={() => onTypeChange('SMART')}>Smartwatch</Link>
          </div>
          <div className="nav-account">
            {status === 'loading' ? <span className="nav-session-loading">Checking session...</span> : user ? <>
              {user.role === 'CUSTOMER' && <><NavLink to="/orders" className="orders-link">Order History</NavLink><NavLink to="/cart" className="cart-link"><Icon name="bag" size={18} /> Shopping Cart</NavLink></>}
              {user.role === 'ADMIN' && <NavLink to="/admin" className="orders-link">Admin Dashboard</NavLink>}
              <span className="nav-user" title={user.fullName}>{user.fullName}</span>
              <button className="nav-logout" onClick={signOut} disabled={busy}>{busy ? 'Signing out...' : 'Log Out'}</button>
            </> : <><NavLink to="/login" className="nav-login">Log In</NavLink><NavLink to="/register" className="nav-cta">Register <Icon name="arrow" size={16} /></NavLink></>}
          </div>
        </nav>
        {error && <div className="container"><Notice>{error}</Notice></div>}
      </header>
    </>
  );
}
