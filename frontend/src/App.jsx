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
import AdminLayout from './pages/admin/AdminLayout.jsx';
import AdminOverview from './pages/admin/AdminOverview.jsx';
import AdminProducts from './pages/admin/AdminProducts.jsx';
import AdminProductForm from './pages/admin/AdminProductForm.jsx';
import AdminOrders from './pages/admin/AdminOrders.jsx';
import AdminOrderDetails from './pages/admin/AdminOrderDetails.jsx';
import { useAuth } from './context/AuthContext.jsx';

// Hello, testing

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
      <a className="skip-link" href="#main-content">Skip to main content</a>
      <Navbar onTypeChange={setWatchType} />
      {(notice || authError) && <div className="container session-notice"><Notice variant={authError ? 'error' : 'info'}>{authError || notice}{authError ? <button onClick={() => refreshSession()}>Check session again</button> : <button onClick={clearNotice} aria-label="Dismiss session notification">×</button>}</Notice></div>}
      <main id="main-content">
        <Routes>
          <Route path="/" element={<Catalog watchType={watchType} onTypeChange={setWatchType} />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/cart" element={<ProtectedRoute><Cart /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
          <Route path="/orders/:id" element={<ProtectedRoute><OrderDetails /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute role="ADMIN"><AdminLayout /></ProtectedRoute>}>
            <Route index element={<AdminOverview />} />
            <Route path="products" element={<AdminProducts />} />
            <Route path="products/new" element={<AdminProductForm />} />
            <Route path="products/:id/edit" element={<AdminProductForm />} />
            <Route path="orders" element={<AdminOrders />} />
            <Route path="orders/:id" element={<AdminOrderDetails />} />
          </Route>
          <Route path="*" element={<section className="route-state container"><h1>This page could not be found.</h1><Link className="button button-dark" to="/">Back to Home</Link></section>} />
        </Routes>
      </main>
      <footer className="site-footer"><div className="container footer-inner"><Link className="footer-brand" to="/">HORA<span>WATCH STORE</span></Link><p>One timepiece. Your own story.</p><span>© {new Date().getFullYear()} HORA</span></div></footer>
    </div>
  );
}
