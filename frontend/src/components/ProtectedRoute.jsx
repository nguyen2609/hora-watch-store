import { Link, Navigate, useLocation } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';

export default function ProtectedRoute({ children, role = 'CUSTOMER' }) {
  const { user, status, refreshSession } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <div className="route-state container" role="status">Checking your session...</div>;
  if (status === 'error') return <div className="route-state container"><h1>Unable to verify your session.</h1><button className="button button-dark" onClick={() => refreshSession()}>Try Again</button></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (user.role !== role) return <div className="route-state container"><h1>{role === 'ADMIN' ? 'You do not have access to the Admin Dashboard.' : 'This page is available to customer accounts only.'}</h1><Link className="button button-dark" to="/">Back to Products</Link></div>;
  return children;
}
