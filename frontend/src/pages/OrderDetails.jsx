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
      <Link className="back-to-orders" to="/orders">← Lịch sử đơn hàng</Link>
      <div className="section-heading"><div><span className="eyebrow">THE DETAILS MATTER</span><h1 id="order-detail-heading">Chi tiết đơn hàng{order ? ` #${order.id}` : ''}</h1></div>{order && !loading && !error && <OrderStatus status={order.status} />}</div>
      {loading ? <div className="route-state" role="status">Đang tải chi tiết đơn hàng...</div> : error ? <Notice>{error} <button onClick={() => setRetry((value) => value + 1)}>Tải lại chi tiết</button></Notice> : order && (
        <>
          <time className="order-date" dateTime={order.createdAt}>Đặt ngày {formatDate(order.createdAt)}</time>
          <div className="order-details-layout"><div className="ordered-products"><h2>Những thiết kế bạn đã chọn</h2><OrderItems items={order.items} /><div className="checkout-total"><span>Tổng tiền đơn hàng</span><strong>{formatPrice(order.totalAmount)}</strong></div><p className="historical-price-note">Đơn giá được giữ theo thời điểm mua hàng.</p></div><aside className="order-delivery"><h2>Thông tin nhận hàng</h2><dl><dt>Người nhận</dt><dd>{order.recipientName}</dd><dt>Số điện thoại</dt><dd>{order.recipientPhone}</dd><dt>Địa chỉ</dt><dd className="shipping-address">{order.shippingAddress}</dd><dt>Phương thức thanh toán</dt><dd>{order.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng (COD)' : order.paymentMethod}</dd><dt>Trạng thái thanh toán</dt><dd>{paymentStatus(order.payment.status)}</dd>{order.payment.paidAt && <><dt>Thanh toán lúc</dt><dd>{formatDate(order.payment.paidAt)}</dd></>}</dl></aside></div>
        </>
      )}
    </section>
  );
}
