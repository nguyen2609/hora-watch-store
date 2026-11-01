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
  return <section><div className="admin-heading"><div><span className="eyebrow">FROM ATELIER TO WRIST</span><h2>Đơn hàng & COD</h2><p>Theo dõi hành trình giao hàng và thanh toán.</p></div></div><div className="admin-filters"><label>Tìm đơn hàng<input id="admin-order-search" type="search" value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Mã đơn hoặc tên người nhận" /></label><label>Trạng thái<select value={status} onChange={event => setStatus(event.target.value)}><option value="ALL">Tất cả trạng thái</option><option value="PENDING">Chờ xác nhận</option><option value="CONFIRMED">Đã xác nhận</option><option value="SHIPPING">Đang giao</option><option value="DELIVERED">Đã giao</option><option value="CANCELLED">Đã hủy</option></select></label><button className="button button-outline" disabled={orders.loading} onClick={orders.reload}>Làm mới</button></div>
    <AdminLoadState {...orders} />{orders.data && <><p className="admin-count">{visible.length} đơn hàng</p><div className="admin-orders">{visible.map(order => <article className="admin-order-card" key={order.id}><div><Link className="order-number" to={`/admin/orders/${order.id}`}>Đơn hàng #{order.id} ↗</Link><time dateTime={order.createdAt}>{formatDate(order.createdAt)}</time><p>{order.recipientName}</p></div><div><OrderStatus status={order.status} /><span>{paymentStatus(order.payment?.status)} · COD</span><strong>{formatPrice(order.totalAmount)}</strong></div></article>)}</div>{visible.length === 0 && <div className="admin-empty">Không có đơn hàng phù hợp.</div>}</>}
  </section>;
}
