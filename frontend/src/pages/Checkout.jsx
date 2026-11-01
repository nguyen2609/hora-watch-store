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
    if (!form.recipientName.trim() || [...form.recipientName.trim()].length > 100) nextErrors.recipientName = 'Enter a recipient name of up to 100 characters.';
    if (!form.recipientPhone.trim() || [...form.recipientPhone.trim()].length > 20) nextErrors.recipientPhone = 'Enter a recipient phone number of up to 20 characters.';
    if (!form.shippingAddress.trim()) nextErrors.shippingAddress = 'Please enter a shipping address.';
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
        setError('The order outcome is uncertain. Check Order History and your shopping cart before trying again to avoid placing a duplicate order.');
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
      <h1 id="confirmation-heading">Your order has been placed.</h1>
      <p>Order <strong>#{order.id}</strong> has been received.</p>
      <OrderStatus status={order.status} />
      <div className="confirmation-total"><span>Order Total</span><strong>{formatPrice(order.totalAmount)}</strong></div>
      <p>Cash on Delivery (COD) · {paymentStatus(order.payment.status)}</p>
      <div className="page-actions"><Link className="button button-dark" to={`/orders/${order.id}`}>View Order Details <Icon name="arrow" size={17} /></Link><Link className="button button-outline" to="/orders">Order History</Link></div>
    </section>
  );

  return (
    <section className="checkout-page container" aria-labelledby="checkout-heading">
      <div className="section-heading"><div><span className="eyebrow">ONE STEP CLOSER</span><h1 id="checkout-heading">Checkout</h1></div><Link className="continue-link" to="/cart">← Back to Shopping Cart</Link></div>
      {loading ? <div className="route-state" role="status">Checking your shopping cart...</div> : loadError ? <Notice>{loadError} <button onClick={() => setRetry((value) => value + 1)}>Try Again</button></Notice> : !cart?.items.length ? (
        <div className="catalog-state"><Icon name="bag" size={35} /><h2>Your shopping cart is empty.</h2><p>Explore our products or review the orders you have already placed.</p><div className="page-actions"><Link className="button button-dark" to="/#collection">Browse Products</Link><Link className="button button-outline" to="/orders">Order History</Link></div></div>
      ) : (
        <>
          {error && <Notice>{error}{uncertain && <Link to="/orders"> Check Order History →</Link>}</Notice>}
          <div className="checkout-layout" aria-busy={busy}>
            <form className="shipping-form account-form" onSubmit={placeOrder} noValidate>
              <span className="eyebrow">SHIPPING DETAILS</span>
              <FormField id="recipient-name" label="Recipient Name" autoComplete="shipping name" value={form.recipientName} onChange={(event) => update('recipientName', event.target.value)} error={errors.recipientName} disabled={busy || uncertain} required />
              <FormField id="recipient-phone" label="Phone Number" type="tel" autoComplete="shipping tel" value={form.recipientPhone} onChange={(event) => update('recipientPhone', event.target.value)} error={errors.recipientPhone} disabled={busy || uncertain} required />
              <div className="form-field"><label htmlFor="shipping-address">Shipping Address</label><textarea id="shipping-address" rows="4" autoComplete="shipping street-address" value={form.shippingAddress} onChange={(event) => update('shippingAddress', event.target.value)} aria-invalid={Boolean(errors.shippingAddress)} aria-describedby={errors.shippingAddress ? 'shipping-address-error' : undefined} disabled={busy || uncertain} required />{errors.shippingAddress && <small id="shipping-address-error" className="field-error">{errors.shippingAddress}</small>}</div>
              <fieldset className="cod-method"><legend>Payment Method</legend><label><input type="radio" checked readOnly name="payment-display" /> Cash on Delivery (COD)</label><p>Pay when your order is delivered.</p></fieldset>
              {cart.items.some((item) => !item.available) && <Notice>A product in your shopping cart has been discontinued. Please update your shopping cart.</Notice>}
              <button type="submit" className="button button-dark place-order" disabled={busy || uncertain || cart.items.some((item) => !item.available)}>{busy ? 'Placing order...' : uncertain ? 'Please Check Order History' : 'Place Order'}<Icon name="arrow" size={18} /></button>
              <p className="checkout-note">Prices and stock are confirmed when you place your order. Keep this page open while your order is being submitted.</p>
            </form>
            <aside className="checkout-review"><div className="review-heading"><h2>Your Order</h2><span>{cart.items.length} {cart.items.length === 1 ? 'product' : 'products'}</span></div><OrderItems items={cart.items} /><div className="checkout-total"><span>Estimated Total</span><strong>{formatPrice(cart.totalAmount)}</strong></div><Link className="edit-cart" to="/cart">Edit Shopping Cart</Link></aside>
          </div>
        </>
      )}
    </section>
  );
}
