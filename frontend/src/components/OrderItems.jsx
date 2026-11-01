import { formatPrice } from '../utils/format.js';

/** Values are supplied by CartResponse or OrderResponse, never recalculated from catalog prices. */
export default function OrderItems({ items }) {
  return (
    <div className="purchase-items">
      {items.map((item) => (
        <article key={item.watchId} className="purchase-item" aria-label={item.name}>
          <div className="purchase-product"><h3>{item.name}</h3>{item.available === false && <span className="cart-unavailable">Sản phẩm không còn được bán</span>}</div>
          <div className="purchase-quantity"><span>Số lượng</span><strong>{item.quantity}</strong></div>
          <div className="purchase-price"><span>Đơn giá</span><strong>{formatPrice(item.unitPrice)}</strong></div>
          <div className="purchase-subtotal"><span>Thành tiền</span><strong>{formatPrice(item.subtotal)}</strong></div>
        </article>
      ))}
    </div>
  );
}
