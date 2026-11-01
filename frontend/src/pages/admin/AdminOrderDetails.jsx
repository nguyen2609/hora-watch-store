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
    } catch { setStockError('Đơn đã hủy thành công; chưa tải được tồn kho hiện tại.'); }
  }
  async function submit(newStatus) {
    if (pending.current || uncertain) return;
    pending.current = true; setBusy(true); setError(''); setMessage('');
    try {
      if (newStatus) {
        const updated = await request(`/api/admin/orders/${id}/status`, { method: 'PATCH', body: { status: newStatus } });
        source.setData(updated);
        setConfirmCancel(false);
        setMessage(newStatus === 'CANCELLED' ? 'Đã hủy đơn. Java đã hoàn kho trong cùng transaction.' : 'Đã cập nhật trạng thái đơn hàng.');
        if (newStatus === 'CANCELLED') await readStock(updated.items);
      } else {
        const payment = await request(`/api/admin/orders/${id}/payment`, { method: 'PATCH' });
        source.setData(current => ({ ...current, payment }));
        setMessage('Đã xác nhận thu tiền COD.');
      }
    } catch (failure) {
      setUncertain(true);
      setError(!failure.status || failure.status >= 500 ? 'Chưa xác định được kết quả thao tác. Tải lại đơn để kiểm tra trước khi gửi tiếp.' : errorMessage(failure, 'admin'));
    } finally { pending.current = false; setBusy(false); }
  }
  function reload() { setUncertain(false); setConfirmCancel(false); setError(''); setMessage(''); source.reload(); }
  return <section><Link className="back-to-orders" to="/admin/orders">← Danh sách đơn hàng</Link><div className="admin-heading"><div><span className="eyebrow">EVERY ORDER MATTERS</span><h2>Đơn hàng #{id}</h2>{order && <p>{formatDate(order.createdAt)}</p>}</div>{order && <OrderStatus status={order.status} />}</div>
    <AdminLoadState {...source} />{message && <Notice variant="success">{message}</Notice>}{error && <Notice>{error}</Notice>}
    {order && <><div className="admin-order-detail"><div className="ordered-products"><h3>Sản phẩm đã mua</h3><OrderItems items={order.items} /><div className="checkout-total"><span>Tổng tiền</span><strong>{formatPrice(order.totalAmount)}</strong></div><p className="historical-price-note">Đơn giá tại thời điểm mua, do máy chủ cung cấp.</p></div><aside className="order-delivery"><h3>Thông tin giao hàng</h3><dl><dt>Người nhận</dt><dd>{order.recipientName}</dd><dt>Điện thoại</dt><dd>{order.recipientPhone}</dd><dt>Địa chỉ</dt><dd className="shipping-address">{order.shippingAddress}</dd><dt>Thanh toán</dt><dd>{order.paymentMethod} · {paymentStatus(order.payment?.status)}</dd>{order.payment?.paidAt && <><dt>Thanh toán lúc</dt><dd data-paid-at>{formatDate(order.payment.paidAt)}</dd></>}</dl></aside></div>
      <div className="admin-operations"><h3>Xử lý đơn hàng</h3><p>Chọn thao tác phù hợp. Máy chủ kiểm tra quyền và trạng thái hiện tại trước khi cập nhật.</p><div className="page-actions">
        {/* These controls are UI hints; OrderService remains the transition authority. */}
        {order.status === 'PENDING' && <button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('CONFIRMED')}>Xác nhận đơn</button>}
        {order.status === 'CONFIRMED' && <button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('SHIPPING')}>Bắt đầu giao hàng</button>}
        {order.status === 'SHIPPING' && <button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('DELIVERED')}>Xác nhận đã giao</button>}
        {['PENDING', 'CONFIRMED'].includes(order.status) && <button className="button button-outline" disabled={busy || uncertain} onClick={() => setConfirmCancel(true)}>Hủy đơn hàng</button>}
        <button className="button button-outline" disabled={busy} onClick={reload}>Tải lại đơn</button>
      </div>{confirmCancel && <div className="admin-confirm" role="group" aria-label="Xác nhận hủy đơn"><p>Hủy đơn #{id} và hoàn lại hàng vào kho?</p><button className="button button-dark" disabled={busy || uncertain} onClick={() => submit('CANCELLED')}>Xác nhận hủy</button><button className="button button-outline" disabled={busy} onClick={() => setConfirmCancel(false)}>Giữ đơn</button></div>}
      {busy && <p role="status">Đang xử lý, vui lòng chờ...</p>}</div>
      {(restored || stockError) && <div className="admin-restored"><h3>Kết quả hoàn kho</h3>{stockError && <Notice>{stockError} <button disabled={busy} onClick={() => readStock(order.items)}>Tải tồn kho</button></Notice>}{restored && <><ul>{restored.map(item => <li key={item.watchId}>{item.name}: hoàn {item.quantity}, tồn hiện tại: {item.stock ?? 'chưa rõ'}</li>)}</ul><small>Tồn kho được đọc lại từ API sau khi hủy; có thể thay đổi bởi giao dịch khác.</small></>}</div>}
      <div className="admin-operations admin-payment"><span className="eyebrow">CASH ON DELIVERY</span><h3>Xác nhận thanh toán COD</h3><p><strong>{paymentStatus(order.payment?.status)}</strong> · {formatPrice(order.payment?.amount)}</p>{order.payment?.paidAt && <p>Đã thu tiền lúc {formatDate(order.payment.paidAt)}</p>}<button className="button button-dark" disabled={busy || uncertain || order.status !== 'DELIVERED' || order.payment?.status !== 'UNPAID' || order.payment?.method !== 'COD'} onClick={() => submit(null)}>Xác nhận thu COD</button><p className="field-hint">Chỉ xác nhận sau khi đơn đã giao và chưa thanh toán.</p></div>
    </>}
  </section>;
}

export default function AdminOrderDetails() {
  const { id } = useParams();
  return isOrderId(id) ? <OrderEditor key={id} id={id} /> : <Notice>Mã đơn hàng không hợp lệ.</Notice>;
}
