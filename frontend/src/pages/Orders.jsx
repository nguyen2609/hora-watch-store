import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { ApiError, errorMessage } from '../api/client.js';
import { formatDate, formatPrice } from '../utils/format.js';
import OrderStatus, { paymentStatus } from '../components/OrderStatus.jsx';
import Notice from '../components/Notice.jsx';
import Icon from '../components/Icon.jsx';

export default function Orders() {
  const { request } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    request('/api/orders/my', { signal: controller.signal })
      .then((data) => {
        if (!Array.isArray(data)) throw new ApiError(502, 'INVALID_RESPONSE');
        if (!controller.signal.aborted) setOrders([...data].sort((a, b) => b.id - a.id));
      })
      .catch((failure) => { if (!controller.signal.aborted) setError(errorMessage(failure, 'orders')); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [request, retry]);

  return (
    <section className="orders-page container" aria-labelledby="orders-heading">
      <div className="section-heading"><div><span className="eyebrow">YOUR TIME, YOUR STORY</span><h1 id="orders-heading">Những đơn hàng của bạn.</h1></div><Link className="continue-link" to="/#collection">Khám phá bộ sưu tập <Icon name="arrow" size={17} /></Link></div>
      {loading ? <div className="route-state" role="status">Đang tải lịch sử đơn hàng...</div> : error ? <Notice>{error} <button onClick={() => setRetry((value) => value + 1)}>Tải lại lịch sử</button></Notice> : orders.length === 0 ? (
        <div className="catalog-state"><Icon name="clock" size={36} /><h2>Bạn chưa có đơn hàng nào.</h2><p>Mỗi lựa chọn đều mở đầu một câu chuyện. Tìm thiết kế của bạn trong bộ sưu tập.</p><Link className="button button-dark" to="/#collection">Khám phá đồng hồ</Link></div>
      ) : (
        <div className="order-history" aria-label="Danh sách đơn hàng">
          {orders.map((order) => <article className="order-history-card" key={order.id} aria-label={`Đơn hàng ${order.id}`}><div className="history-card-heading"><div><Link className="order-number" to={`/orders/${order.id}`}>Đơn #{order.id}</Link><time dateTime={order.createdAt}>{formatDate(order.createdAt)}</time></div><OrderStatus status={order.status} /></div><div className="history-products">{order.items.slice(0, 2).map((item) => <p key={item.watchId}>{item.name} <span>× {item.quantity}</span></p>)}{order.items.length > 2 && <small>Và {order.items.length - 2} sản phẩm khác</small>}</div><div className="history-card-bottom"><span>{order.paymentMethod} · {paymentStatus(order.payment.status)}</span><strong>{formatPrice(order.totalAmount)}</strong><Link to={`/orders/${order.id}`}>Xem chi tiết <Icon name="arrow" size={16} /></Link></div></article>)}
        </div>
      )}
    </section>
  );
}
