import { Link } from 'react-router';
import useAdminData from '../../hooks/useAdminData.js';
import AdminLoadState from '../../components/AdminLoadState.jsx';

export default function AdminOverview() {
  const products = useAdminData('/api/admin/watches');
  const orders = useAdminData('/api/admin/orders');
  return <section><div className="admin-heading"><div><span className="eyebrow">THE BIG PICTURE</span><h2>Store Overview</h2><p>Products, orders and payments in one place.</p></div></div>
    <AdminLoadState {...products} /><AdminLoadState {...orders} />
    {products.data && orders.data && <><div className="admin-stats"><article><span>Total Products</span><strong>{products.data.length}</strong><small>{products.data.filter(watch => watch.available).length} active</small></article><article><span>Pending Orders</span><strong>{orders.data.filter(order => order.status === 'PENDING').length}</strong><small>{orders.data.length} total {orders.data.length === 1 ? 'order' : 'orders'}</small></article><article><span>COD Awaiting Confirmation</span><strong>{orders.data.filter(order => order.status === 'DELIVERED' && order.payment?.status === 'UNPAID').length}</strong><small>Delivered orders awaiting payment</small></article></div><div className="admin-quick-links"><Link to="/admin/products"><span>01 / COLLECTION</span><h3>Product Management ↗</h3><p>Update product details, prices and stock.</p></Link><Link to="/admin/orders"><span>02 / OPERATIONS</span><h3>Order Management ↗</h3><p>Track deliveries and confirm Cash on Delivery payments.</p></Link></div></>}
  </section>;
}
