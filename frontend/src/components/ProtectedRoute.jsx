import { Link, Navigate, useLocation } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';

export default function ProtectedRoute({ children, role = 'CUSTOMER' }) {
  const { user, status, refreshSession } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <div className="route-state container" role="status">Đang kiểm tra phiên đăng nhập...</div>;
  if (status === 'error') return <div className="route-state container"><h1>Chưa thể xác minh phiên đăng nhập.</h1><button className="button button-dark" onClick={() => refreshSession()}>Thử lại</button></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (user.role !== role) return <div className="route-state container"><h1>{role === 'ADMIN' ? 'Bạn không có quyền quản trị.' : 'Trang này dành cho tài khoản khách hàng.'}</h1><Link className="button button-dark" to="/">Về bộ sưu tập</Link></div>;
  return children;
}
