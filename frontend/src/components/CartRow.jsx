import { useEffect, useState } from 'react';
import { formatPrice } from '../utils/format.js';
import Icon from './Icon.jsx';

export default function CartRow({ item, busy, onUpdate, onRemove }) {
  const [quantity, setQuantity] = useState(String(item.quantity));
  const [error, setError] = useState('');
  useEffect(() => { setQuantity(String(item.quantity)); setError(''); }, [item.quantity]);

  function submit(event) {
    event.preventDefault();
    if (!/^[1-9][0-9]*$/.test(quantity) || Number(quantity) > 2147483647) {
      setError('Nhập số lượng nguyên dương hợp lệ.');
      return;
    }
    setError('');
    onUpdate(item.watchId, Number(quantity));
  }

  return (
    <article className="cart-row" aria-label={item.name}>
      <div className="cart-product"><span className="cart-product-icon"><Icon name="clock" size={30} /></span><div><h2>{item.name}</h2><p>{formatPrice(item.unitPrice)} / sản phẩm</p>{!item.available && <span className="cart-unavailable">Sản phẩm hiện không còn được bán</span>}</div></div>
      <div className="cart-quantity">
        <form onSubmit={submit} noValidate>
          <label htmlFor={`quantity-${item.watchId}`}>Số lượng</label>
          <div><input id={`quantity-${item.watchId}`} type="number" min="1" step="1" inputMode="numeric" value={quantity} onChange={(event) => setQuantity(event.target.value)} aria-label={`Số lượng ${item.name}`} aria-invalid={Boolean(error)} aria-describedby={error ? `quantity-error-${item.watchId}` : undefined} disabled={busy} /><button type="submit" disabled={busy || !item.available} aria-label={`Cập nhật ${item.name}`}>Cập nhật</button></div>
          {error && <small id={`quantity-error-${item.watchId}`} className="field-error">{error}</small>}
        </form>
      </div>
      <div className="cart-line-total"><span>Thành tiền</span><strong>{formatPrice(item.subtotal)}</strong><button type="button" onClick={() => onRemove(item.watchId)} disabled={busy} aria-label={`Xóa ${item.name}`}>Xóa sản phẩm</button></div>
    </article>
  );
}
