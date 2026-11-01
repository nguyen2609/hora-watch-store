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
      setError('Enter a valid positive whole-number quantity.');
      return;
    }
    setError('');
    onUpdate(item.watchId, Number(quantity));
  }

  return (
    <article className="cart-row" aria-label={item.name}>
      <div className="cart-product"><span className="cart-product-icon"><Icon name="clock" size={30} /></span><div><h2>{item.name}</h2><p>{formatPrice(item.unitPrice)} / item</p>{!item.available && <span className="cart-unavailable">This product has been discontinued</span>}</div></div>
      <div className="cart-quantity">
        <form onSubmit={submit} noValidate>
          <label htmlFor={`quantity-${item.watchId}`}>Quantity</label>
          <div><input id={`quantity-${item.watchId}`} type="number" min="1" step="1" inputMode="numeric" value={quantity} onChange={(event) => setQuantity(event.target.value)} aria-label={`Quantity for ${item.name}`} aria-invalid={Boolean(error)} aria-describedby={error ? `quantity-error-${item.watchId}` : undefined} disabled={busy} /><button type="submit" disabled={busy || !item.available} aria-label={`Update ${item.name}`}>Update</button></div>
          {error && <small id={`quantity-error-${item.watchId}`} className="field-error">{error}</small>}
        </form>
      </div>
      <div className="cart-line-total"><span>Subtotal</span><strong>{formatPrice(item.subtotal)}</strong><button type="button" onClick={() => onRemove(item.watchId)} disabled={busy} aria-label={`Remove ${item.name}`}>Remove Product</button></div>
    </article>
  );
}
