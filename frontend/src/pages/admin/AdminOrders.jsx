import { useState } from 'react';
import { Link } from 'react-router';
import useAdminData from '../../hooks/useAdminData.js';
import AdminLoadState from '../../components/AdminLoadState.jsx';
import OrderStatus, { paymentStatus } from '../../components/OrderStatus.jsx';
import { formatDate, formatPrice } from '../../utils/format.js';

export default function AdminOrders() {
  const orders = useAdminData('/api/admin/orders');
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('ALL');
  const visible = (orders.data || []).filter(order => (status === 'ALL' || order.status === status) && `${order.id} ${order.recipientName}`.toLowerCase().includes(keyword.toLowerCase())).toReversed();
  return <section><div className="admin-heading"><div><span className="eyebrow">FROM ATELIER TO WRIST</span><h2>Order Management</h2><p>Track order deliveries and payments.</p></div></div><div className="admin-filters"><label>Search Orders<input id="admin-order-search" type="search" value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Order ID or recipient name" /></label><label>Status<select value={status} onChange={event => setStatus(event.target.value)}><option value="ALL">All Statuses</option><option value="PENDING">Pending</option><option value="CONFIRMED">Confirmed</option><option value="SHIPPING">Shipping</option><option value="DELIVERED">Delivered</option><option value="CANCELLED">Cancelled</option></select></label><button className="button button-outline" disabled={orders.loading} onClick={orders.reload}>Refresh</button></div>
    <AdminLoadState {...orders} />{orders.data && <><p className="admin-count">{visible.length} {visible.length === 1 ? 'order' : 'orders'}</p><div className="admin-orders">{visible.map(order => <article className="admin-order-card" key={order.id}><div><Link className="order-number" to={`/admin/orders/${order.id}`}>Order #{order.id} ↗</Link><time dateTime={order.createdAt}>{formatDate(order.createdAt)}</time><p>{order.recipientName}</p></div><div><OrderStatus status={order.status} /><span>{paymentStatus(order.payment?.status)} · Cash on Delivery</span><strong>{formatPrice(order.totalAmount)}</strong></div></article>)}</div>{visible.length === 0 && <div className="admin-empty">No matching orders found.</div>}</>}
  </section>;
}
