import { useEffect, useState } from 'react';
import { Link, Route, Routes, useLocation } from 'react-router';
import Navbar from './components/Navbar.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Notice from './components/Notice.jsx';
import Catalog from './pages/Catalog.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Cart from './pages/Cart.jsx';
import Checkout from './pages/Checkout.jsx';
import Orders from './pages/Orders.jsx';
import OrderDetails from './pages/OrderDetails.jsx';
import { useAuth } from './context/AuthContext.jsx';

export default function App() {
  const [watchType, setWatchType] = useState('ALL');
  const { authError, notice, refreshSession, clearNotice } = useAuth();
  const location = useLocation();

  useEffect(() => {
    if (location.hash === '#collection') {
      document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth' });
    } else {
      window.scrollTo(0, 0);
    }
  }, [location]);

  return (
    <div id="home" className="app-layout">
      <a className="skip-link" href="#main-content">Đến nội dung chính</a>
      <Navbar onTypeChange={setWatchType} />
      {(notice || authError) && <div className="container session-notice"><Notice variant={authError ? 'error' : 'info'}>{authError || notice}{authError ? <button onClick={() => refreshSession()}>Kiểm tra lại phiên</button> : <button onClick={clearNotice} aria-label="Đóng thông báo phiên">×</button>}</Notice></div>}
      <main id="main-content">
        <Routes>
          <Route path="/" element={<Catalog watchType={watchType} onTypeChange={setWatchType} />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/cart" element={<ProtectedRoute><Cart /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
          <Route path="/orders/:id" element={<ProtectedRoute><OrderDetails /></ProtectedRoute>} />
          <Route path="*" element={<section className="route-state container"><h1>Trang này chưa có trong bộ sưu tập.</h1><Link className="button button-dark" to="/">Về trang chủ</Link></section>} />
        </Routes>
      </main>
      <footer className="site-footer"><div className="container footer-inner"><Link className="footer-brand" to="/">HORA<span>WATCH STORE</span></Link><p>Một chiếc đồng hồ. Một câu chuyện riêng.</p><span>© {new Date().getFullYear()} HORA</span></div></footer>
    </div>
  );
}
