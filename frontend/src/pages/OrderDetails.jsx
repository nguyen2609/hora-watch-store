import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { ApiError, errorMessage } from '../api/client.js';
import { formatDate, formatPrice } from '../utils/format.js';
import { isOrderId } from '../utils/navigation.js';
import OrderItems from '../components/OrderItems.jsx';
import OrderStatus, { paymentStatus } from '../components/OrderStatus.jsx';
import Notice from '../components/Notice.jsx';

export default function OrderDetails() {
  const { id } = useParams();
  const { request } = useAuth();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    setOrder(null);
    async function load() {
      try {
        if (!isOrderId(id)) throw new ApiError(404, 'NOT_FOUND');
        const data = await request(`/api/orders/${id}`, { signal: controller.signal });
        if (!controller.signal.aborted) setOrder(data);
      } catch (failure) {
        if (!controller.signal.aborted) setError(errorMessage(failure, 'orders'));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    load();
    return () => controller.abort();
  }, [id, request, retry]);

  return (
    <section className="order-details-page container" aria-labelledby="order-detail-heading">
      <Link className="back-to-orders" to="/orders">← Order History</Link>
      <div className="section-heading"><div><span className="eyebrow">THE DETAILS MATTER</span><h1 id="order-detail-heading">Order Details{order ? ` #${order.id}` : ''}</h1></div>{order && !loading && !error && <OrderStatus status={order.status} />}</div>
      {loading ? <div className="route-state" role="status">Loading order details...</div> : error ? <Notice>{error} <button onClick={() => setRetry((value) => value + 1)}>Reload Details</button></Notice> : order && (
        <>
          <time className="order-date" dateTime={order.createdAt}>Placed on {formatDate(order.createdAt)}</time>
          <div className="order-details-layout"><div className="ordered-products"><h2>Your Selected Products</h2><OrderItems items={order.items} /><div className="checkout-total"><span>Order Total</span><strong>{formatPrice(order.totalAmount)}</strong></div><p className="historical-price-note">Unit prices are preserved from the time of purchase.</p></div><aside className="order-delivery"><h2>Shipping Details</h2><dl><dt>Recipient</dt><dd>{order.recipientName}</dd><dt>Phone Number</dt><dd>{order.recipientPhone}</dd><dt>Address</dt><dd className="shipping-address">{order.shippingAddress}</dd><dt>Payment Method</dt><dd>{order.paymentMethod === 'COD' ? 'Cash on Delivery (COD)' : order.paymentMethod}</dd><dt>Payment Status</dt><dd>{paymentStatus(order.payment.status)}</dd>{order.payment.paidAt && <><dt>Paid At</dt><dd>{formatDate(order.payment.paidAt)}</dd></>}</dl></aside></div>
        </>
      )}
    </section>
  );
}
