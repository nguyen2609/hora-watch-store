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
      <div className="section-heading"><div><span className="eyebrow">YOUR TIME, YOUR STORY</span><h1 id="orders-heading">Order History</h1></div><Link className="continue-link" to="/#collection">Explore Products <Icon name="arrow" size={17} /></Link></div>
      {loading ? <div className="route-state" role="status">Loading Order History...</div> : error ? <Notice>{error} <button onClick={() => setRetry((value) => value + 1)}>Reload Order History</button></Notice> : orders.length === 0 ? (
        <div className="catalog-state"><Icon name="clock" size={36} /><h2>You have no orders yet.</h2><p>Every choice starts a story. Find your next timepiece in our collection.</p><Link className="button button-dark" to="/#collection">Explore Products</Link></div>
      ) : (
        <div className="order-history" aria-label="Order List">
          {orders.map((order) => <article className="order-history-card" key={order.id} aria-label={`Order ${order.id}`}><div className="history-card-heading"><div><Link className="order-number" to={`/orders/${order.id}`}>Order #{order.id}</Link><time dateTime={order.createdAt}>{formatDate(order.createdAt)}</time></div><OrderStatus status={order.status} /></div><div className="history-products">{order.items.slice(0, 2).map((item) => <p key={item.watchId}>{item.name} <span>× {item.quantity}</span></p>)}{order.items.length > 2 && <small>And {order.items.length - 2} more {order.items.length - 2 === 1 ? 'product' : 'products'}</small>}</div><div className="history-card-bottom"><span>{order.paymentMethod === 'COD' ? 'Cash on Delivery' : order.paymentMethod} · {paymentStatus(order.payment.status)}</span><strong>{formatPrice(order.totalAmount)}</strong><Link to={`/orders/${order.id}`}>View Details <Icon name="arrow" size={16} /></Link></div></article>)}
        </div>
      )}
    </section>
  );
}
