import { useRef, useState } from 'react';
import { Link, useParams } from 'react-router';
import { useAuth } from '../../context/AuthContext.jsx';
import useAdminData from '../../hooks/useAdminData.js';
import AdminLoadState from '../../components/AdminLoadState.jsx';
import OrderItems from '../../components/OrderItems.jsx';
import OrderStatus, { paymentStatus } from '../../components/OrderStatus.jsx';
import Notice from '../../components/Notice.jsx';
import { formatDate, formatPrice } from '../../utils/format.js';
import { errorMessage } from '../../api/client.js';
import { isOrderId } from '../../utils/navigation.js';

function OrderEditor({ id }) {
  const source = useAdminData(`/api/admin/orders/${id}`);
  const { request } = useAuth();
  const [busy, setBusy] = useState(false);
  const [uncertain, setUncertain] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [restored, setRestored] = useState(null);
  const [stockError, setStockError] = useState('');
  const pending = useRef(false);
  const order = source.data;
  async function readStock(items) {
    setStockError('');
    try {
      const watches = await request('/api/admin/watches');
      setRestored(items.map(item => ({ ...item, stock: watches.find(watch => watch.id === item.watchId)?.stockQuantity })));
    } catch { setStockError('The order was cancelled successfully, but current stock could not be loaded.'); }
  }
  async function submit(newStatus) {
    if (pending.current || uncertain) return;
    pending.current = true; setBusy(true); setError(''); setMessage('');
    try {
      if (newStatus) {
        const updated = await request(`/api/admin/orders/${id}/status`, { method: 'PATCH', body: { status: newStatus } });
        source.setData(updated);
        setConfirmCancel(false);
        setMessage(newStatus === 'CANCELLED' ? 'Order cancelled. Stock was restored in the same transaction.' : 'Order status updated.');
        if (newStatus === 'CANCELLED') await readStock(updated.items);
      } else {
        const payment = await request(`/api/admin/orders/${id}/payment`, { method: 'PATCH' });
        source.setData(current => ({ ...current, payment }));
        setMessage('Cash on Delivery payment confirmed.');
      }
    } catch (failure) {
      setUncertain(true);
      setError(!failure.status || failure.status >= 500 ? 'The action outcome is uncertain. Reload the order to check its status before submitting again.' : errorMessage(failure, 'admin'));
    } finally { pending.current = false; setBusy(false); }
  }
  function reload() { setUncertain(false); setConfirmCancel(false); setError(''); setMessage(''); source.reload(); }
  return <section><Link className="back-to-orders" to="/admin/orders">← Order List</Link><div className="admin-heading"><div><span className="eyebrow">EVERY ORDER MATTERS</span><h2>Order #{id}</h2>{order && <p>{formatDate(order.createdAt)}</p>}</div>{order && <OrderStatus status={order.status} />}</div>
    <AdminLoadState {...source} />{message && <Notice variant="success">{message}</Notice>}{error && <Notice>{error}</Notice>}
    {order && <><div className="admin-order-detail"><div className="ordered-products"><h3>Purchased Products</h3><OrderItems items={order.items} /><div className="checkout-total"><span>Total</span><strong>{formatPrice(order.totalAmount)}</strong></div><p className="historical-price-note">Purchase-time unit prices supplied by the server.</p></div><aside className="order-delivery"><h3>Shipping Details</h3><dl><dt>Recipient</dt><dd>{order.recipientName}</dd><dt>Phone Number</dt><dd>{order.recipientPhone}</dd><dt>Address</dt><dd className="shipping-address">{order.shippingAddress}</dd><dt>Payment Status</dt><dd>{order.paymentMethod === 'COD' ? 'Cash on Delivery' : order.paymentMethod} · {paymentStatus(order.payment?.status)}</dd>{order.payment?.paidAt && <><dt>Paid At</dt><dd data-paid-at>{formatDate(order.payment.paidAt)}</dd></>}</dl></aside></div>
      <div className="admin-operations"><h3>Order Actions</h3><p>Choose an action below. The server verifies your permissions and the current order status before updating.</p><div className="page-actions">
        {/* These controls are UI hints; OrderService remains the transition authority. */}
        {order.status === 'PENDING' && <button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('CONFIRMED')}>Confirm Order</button>}
        {order.status === 'CONFIRMED' && <button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('SHIPPING')}>Start Shipping</button>}
        {order.status === 'SHIPPING' && <button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('DELIVERED')}>Mark as Delivered</button>}
        {['PENDING', 'CONFIRMED'].includes(order.status) && <button className="button button-outline" disabled={busy || uncertain} onClick={() => setConfirmCancel(true)}>Cancel Order</button>}
        <button className="button button-outline" disabled={busy} onClick={reload}>Reload Order</button>
      </div>{confirmCancel && <div className="admin-confirm" role="group" aria-label="Confirm Order Cancellation"><p>Cancel order #{id} and restore its items to stock?</p><button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('CANCELLED')}>Confirm Cancellation</button><button className="button button-outline" disabled={busy} onClick={() => setConfirmCancel(false)}>Keep Order</button></div>}
      {busy && <p role="status">Processing, please wait...</p>}</div>
      {(restored || stockError) && <div className="admin-restored"><h3>Stock Restoration</h3>{stockError && <Notice>{stockError} <button disabled={busy} onClick={() => readStock(order.items)}>Reload Stock</button></Notice>}{restored && <><ul>{restored.map(item => <li key={item.watchId}>{item.name}: restored {item.quantity}, current stock: {item.stock ?? 'unknown'}</li>)}</ul><small>Stock is refreshed after cancellation and may change due to other transactions.</small></>}</div>}
      <div className="admin-operations admin-payment"><span className="eyebrow">CASH ON DELIVERY</span><h3>Cash on Delivery Payment</h3><p><strong>{paymentStatus(order.payment?.status)}</strong> · {formatPrice(order.payment?.amount)}</p>{order.payment?.paidAt && <p>Paid At {formatDate(order.payment.paidAt)}</p>}<button className="button button-dark" disabled={busy || uncertain || order.status !== 'DELIVERED' || order.payment?.status !== 'UNPAID' || order.payment?.method !== 'COD'} onClick={() => submit(null)}>Confirm COD Payment</button><p className="field-hint">Confirm only after the order is delivered and while payment is unpaid.</p></div>
    </>}
  </section>;
}

export default function AdminOrderDetails() {
  const { id } = useParams();
  return isOrderId(id) ? <OrderEditor key={id} id={id} /> : <Notice>Invalid order ID.</Notice>;
}
