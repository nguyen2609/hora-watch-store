import { NavLink, Outlet } from 'react-router';
import { useAuth } from '../../context/AuthContext.jsx';
import '../../admin.css';

export default function AdminLayout() {
  const { user } = useAuth();
  return (
    <div className="admin-shell container">
      <aside className="admin-sidebar"><span className="eyebrow">HORA ATELIER</span><h1>Admin Dashboard</h1><p>Welcome, {user.fullName}</p><nav aria-label="Admin Dashboard"><NavLink end to="/admin">Overview</NavLink><NavLink to="/admin/products">Products</NavLink><NavLink to="/admin/orders">Order Management</NavLink></nav><div className="admin-sidebar-note">Care in every product.<br />Precision in every order.</div></aside>
      <div className="admin-content"><Outlet /></div>
    </div>
  );
}
