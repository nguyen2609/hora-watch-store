import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import { formatPrice } from '../utils/format.js';
import CartRow from '../components/CartRow.jsx';
import Notice from '../components/Notice.jsx';
import Icon from '../components/Icon.jsx';

export default function Cart() {
  const { request } = useAuth();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    request('/api/cart', { signal: controller.signal })
      .then((data) => { if (!controller.signal.aborted) setCart(data); })
      .catch((failure) => { if (!controller.signal.aborted) setError(errorMessage(failure, 'cart')); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [request, retry]);

  async function mutate(path, method, body) {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const updated = await request(path, { method, body });
      setCart(updated);
      setNotice('Giỏ hàng đã được cập nhật.');
    } catch (failure) { setError(errorMessage(failure, 'cart')); }
    finally { setBusy(false); }
  }

  return (
    <section className="cart-page container" aria-labelledby="cart-heading">
      <div className="section-heading"><div><span className="eyebrow">YOUR PERSONAL SELECTION</span><h1 id="cart-heading">Những lựa chọn của bạn.</h1></div><Link className="continue-link" to="/#collection">Tiếp tục khám phá <Icon name="arrow" size={17} /></Link></div>
      {error && <Notice>{error} <button onClick={() => setRetry((value) => value + 1)} disabled={busy}>Tải lại giỏ hàng</button></Notice>}
      {notice && <Notice variant="success">{notice}</Notice>}
      {loading ? <div className="route-state" role="status">Đang tải giỏ hàng...</div> : !cart ? null : cart.items.length === 0 ? (
        <div className="catalog-state empty-cart"><span className="state-icon"><Icon name="bag" size={38} /></span><h2>Giỏ hàng còn một khoảng trống.</h2><p>Chọn một thiết kế bạn yêu thích để bắt đầu bộ sưu tập riêng.</p><Link className="button button-dark" to="/#collection">Khám phá bộ sưu tập <Icon name="arrow" size={18} /></Link></div>
      ) : (
        <div className="cart-layout" aria-busy={busy}>
          <div><div className="cart-list-heading"><span>{cart.items.length} sản phẩm khác nhau</span><button disabled={busy} onClick={() => mutate('/api/cart/items', 'DELETE')}>Xóa tất cả</button></div><div className="cart-items">{cart.items.map((item) => <CartRow key={item.watchId} item={item} busy={busy} onUpdate={(id, quantity) => mutate(`/api/cart/items/${id}`, 'PATCH', { quantity })} onRemove={(id) => mutate(`/api/cart/items/${id}`, 'DELETE')} />)}</div></div>
          <aside className="cart-summary"><span className="eyebrow">LỰA CHỌN CỦA BẠN</span><h2>Tóm tắt giỏ hàng</h2><div><span>Tổng số lượng</span><strong>{cart.items.reduce((sum, item) => sum + item.quantity, 0)}</strong></div><div className="cart-grand-total"><span>Tổng tiền</span><strong>{formatPrice(cart.totalAmount)}</strong></div><p>Giá và tồn kho được kiểm tra lại trên hệ thống. Thêm vào giỏ chưa giữ chỗ hoặc trừ kho.</p>{!busy && <Link className="button button-dark" to="/checkout">Tiến hành đặt hàng <Icon name="arrow" size={17} /></Link>}<Link className="cart-continue" to="/#collection">Tiếp tục chọn đồng hồ</Link></aside>
        </div>
      )}
    </section>
  );
}
