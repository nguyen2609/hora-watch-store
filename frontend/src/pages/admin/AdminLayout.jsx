import { NavLink, Outlet } from 'react-router';
import { useAuth } from '../../context/AuthContext.jsx';
import '../../admin.css';

export default function AdminLayout() {
  const { user } = useAuth();
  return (
    <div className="admin-shell container">
      <aside className="admin-sidebar"><span className="eyebrow">HORA ATELIER</span><h1>Quản trị</h1><p>Xin chào, {user.fullName}</p><nav aria-label="Quản trị"><NavLink end to="/admin">Tổng quan</NavLink><NavLink to="/admin/products">Sản phẩm</NavLink><NavLink to="/admin/orders">Đơn hàng & COD</NavLink></nav><div className="admin-sidebar-note">Chăm chút từng sản phẩm.<br />Trọn vẹn từng đơn hàng.</div></aside>
      <div className="admin-content"><Outlet /></div>
    </div>
  );
}
