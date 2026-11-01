import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { ApiError, errorMessage } from '../api/client.js';
import { formatPrice } from '../utils/format.js';
import FormField from '../components/FormField.jsx';
import Notice from '../components/Notice.jsx';
import Icon from '../components/Icon.jsx';
import OrderItems from '../components/OrderItems.jsx';
import OrderStatus, { paymentStatus } from '../components/OrderStatus.jsx';

export default function Checkout() {
  const { user, request } = useAuth();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [error, setError] = useState('');
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [uncertain, setUncertain] = useState(false);
  const [order, setOrder] = useState(null);
  const [retry, setRetry] = useState(0);
  const [form, setForm] = useState({ recipientName: user.fullName || '', recipientPhone: user.phone || '', shippingAddress: '' });
  // A synchronous guard also blocks repeated submit events before React renders disabled=true.
  const submissionLocked = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError('');
    request('/api/cart', { signal: controller.signal })
      .then((data) => {
        if (!data || !Array.isArray(data.items)) throw new ApiError(502, 'INVALID_RESPONSE');
        if (!controller.signal.aborted) setCart(data);
      })
      .catch((failure) => { if (!controller.signal.aborted) setLoadError(errorMessage(failure, 'cart')); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [request, retry]);

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: '' }));
  }

  async function placeOrder(event) {
    event.preventDefault();
    if (submissionLocked.current || order || uncertain || !cart?.items.length) return;
    const nextErrors = {};
    if (!form.recipientName.trim() || [...form.recipientName.trim()].length > 100) nextErrors.recipientName = 'Nhập tên người nhận, tối đa 100 ký tự.';
    if (!form.recipientPhone.trim() || [...form.recipientPhone.trim()].length > 20) nextErrors.recipientPhone = 'Nhập số điện thoại người nhận, tối đa 20 ký tự.';
    if (!form.shippingAddress.trim()) nextErrors.shippingAddress = 'Vui lòng nhập địa chỉ nhận hàng.';
    setErrors(nextErrors);
    setError('');
    if (Object.keys(nextErrors).length) return;

    submissionLocked.current = true;
    setBusy(true);
    let unlock = true;
    try {
      // Only recipient fields are sent. Java owns user identity, prices, stock and the transaction.
      const stored = await request('/api/orders/checkout', {
        method: 'POST',
        body: {
          recipientName: form.recipientName.trim(),
          recipientPhone: form.recipientPhone.trim(),
          shippingAddress: form.shippingAddress.trim(),
        },
      });
      if (!Number.isInteger(stored?.id) || stored.id <= 0) throw new ApiError(502, 'INVALID_RESPONSE');
      unlock = false;
      setOrder(stored);
    } catch (failure) {
      if (!failure.status || failure.status >= 500 || failure.code === 'SESSION_CHANGED') {
        // The server may have committed before a response was lost. Never replay this POST.
        unlock = false;
        setUncertain(true);
        setError('Chưa xác định được kết quả đặt hàng. Hãy kiểm tra lịch sử đơn hàng và giỏ hàng trước khi đặt lại để tránh tạo thêm đơn.');
      } else {
        setError(errorMessage(failure, 'checkout'));
      }
    } finally {
      if (unlock) submissionLocked.current = false;
      setBusy(false);
    }
  }

  if (order) return (
    <section className="order-confirmation container" aria-labelledby="confirmation-heading">
      <span className="confirmation-icon"><Icon name="check" size={38} /></span>
      <span className="eyebrow">THANK YOU FOR CHOOSING HORA</span>
      <h1 id="confirmation-heading">Đặt hàng thành công.</h1>
      <p>Đơn hàng <strong>#{order.id}</strong> đã được ghi nhận.</p>
      <OrderStatus status={order.status} />
      <div className="confirmation-total"><span>Tổng tiền đơn hàng</span><strong>{formatPrice(order.totalAmount)}</strong></div>
      <p>Thanh toán khi nhận hàng (COD) · {paymentStatus(order.payment.status)}</p>
      <div className="page-actions"><Link className="button button-dark" to={`/orders/${order.id}`}>Xem chi tiết đơn hàng <Icon name="arrow" size={17} /></Link><Link className="button button-outline" to="/orders">Lịch sử đơn hàng</Link></div>
    </section>
  );

  return (
    <section className="checkout-page container" aria-labelledby="checkout-heading">
      <div className="section-heading"><div><span className="eyebrow">ONE STEP CLOSER</span><h1 id="checkout-heading">Hoàn tất lựa chọn của bạn.</h1></div><Link className="continue-link" to="/cart">← Quay lại giỏ hàng</Link></div>
      {loading ? <div className="route-state" role="status">Đang kiểm tra giỏ hàng...</div> : loadError ? <Notice>{loadError} <button onClick={() => setRetry((value) => value + 1)}>Thử tải lại</button></Notice> : !cart?.items.length ? (
        <div className="catalog-state"><Icon name="bag" size={35} /><h2>Giỏ hàng đang trống.</h2><p>Khám phá bộ sưu tập hoặc kiểm tra các đơn bạn đã đặt.</p><div className="page-actions"><Link className="button button-dark" to="/#collection">Chọn đồng hồ</Link><Link className="button button-outline" to="/orders">Lịch sử đơn hàng</Link></div></div>
      ) : (
        <>
          {error && <Notice>{error}{uncertain && <Link to="/orders"> Kiểm tra lịch sử đơn hàng →</Link>}</Notice>}
          <div className="checkout-layout" aria-busy={busy}>
            <form className="shipping-form account-form" onSubmit={placeOrder} noValidate>
              <span className="eyebrow">THÔNG TIN NHẬN HÀNG</span>
              <FormField id="recipient-name" label="Tên người nhận" autoComplete="shipping name" value={form.recipientName} onChange={(event) => update('recipientName', event.target.value)} error={errors.recipientName} disabled={busy || uncertain} required />
              <FormField id="recipient-phone" label="Số điện thoại" type="tel" autoComplete="shipping tel" value={form.recipientPhone} onChange={(event) => update('recipientPhone', event.target.value)} error={errors.recipientPhone} disabled={busy || uncertain} required />
              <div className="form-field"><label htmlFor="shipping-address">Địa chỉ nhận hàng</label><textarea id="shipping-address" rows="4" autoComplete="shipping street-address" value={form.shippingAddress} onChange={(event) => update('shippingAddress', event.target.value)} aria-invalid={Boolean(errors.shippingAddress)} aria-describedby={errors.shippingAddress ? 'shipping-address-error' : undefined} disabled={busy || uncertain} required />{errors.shippingAddress && <small id="shipping-address-error" className="field-error">{errors.shippingAddress}</small>}</div>
              <fieldset className="cod-method"><legend>Phương thức thanh toán</legend><label><input type="radio" checked readOnly name="payment-display" /> Thanh toán khi nhận hàng (COD)</label><p>Bạn thanh toán khi đơn hàng được giao.</p></fieldset>
              {cart.items.some((item) => !item.available) && <Notice>Một sản phẩm trong giỏ không còn được bán. Vui lòng điều chỉnh giỏ hàng.</Notice>}
              <button type="submit" className="button button-dark place-order" disabled={busy || uncertain || cart.items.some((item) => !item.available)}>{busy ? 'Đang đặt hàng...' : uncertain ? 'Vui lòng kiểm tra lịch sử đơn' : 'Đặt hàng'}<Icon name="arrow" size={18} /></button>
              <p className="checkout-note">Giá và tồn kho được xác nhận lại khi đặt hàng. Không đóng trang trong lúc đang gửi.</p>
            </form>
            <aside className="checkout-review"><div className="review-heading"><h2>Đơn hàng của bạn</h2><span>{cart.items.length} sản phẩm khác nhau</span></div><OrderItems items={cart.items} /><div className="checkout-total"><span>Tổng tiền dự kiến</span><strong>{formatPrice(cart.totalAmount)}</strong></div><Link className="edit-cart" to="/cart">Điều chỉnh giỏ hàng</Link></aside>
          </div>
        </>
      )}
    </section>
  );
}
