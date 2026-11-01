import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import { formatPrice } from '../utils/format.js';
import Icon from './Icon.jsx';
import automaticPlaceholder from '../assets/watch-automatic.svg';
import smartPlaceholder from '../assets/watch-smart.svg';

function safeImageUrl(value) {
  if (!value || typeof value !== 'string') return null;
  try {
    const url = new URL(value, window.location.origin);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

export default function WatchCard({ watch }) {
  const { user, status, request } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [error, setError] = useState('');
  const [failedImage, setFailedImage] = useState(null);
  const imageUrl = safeImageUrl(watch.imageUrl);
  const hasPhoto = imageUrl && failedImage !== imageUrl;
  const isAutomatic = watch.watchType === 'AUTOMATIC';
  const inStock = watch.stockQuantity > 0;

  async function addToCart() {
    if (!user) {
      navigate('/login', { state: { from: '/#collection', notice: 'Log in to add products to your shopping cart.' } });
      return;
    }
    setBusy(true);
    setFeedback('');
    setError('');
    try {
      // The server determines ownership, current stock and all monetary values.
      await request('/api/cart/items', { method: 'POST', body: { watchId: watch.id, quantity: 1 } });
      setFeedback('Added to your shopping cart.');
    } catch (failure) {
      if (failure.status === 401) navigate('/login', { state: { from: '/#collection' } });
      else setError(errorMessage(failure, 'cart'));
    } finally { setBusy(false); }
  }

  return (
    <article className="watch-card">
      <div className={`watch-image-wrap ${isAutomatic ? 'automatic-image' : 'smart-image'}`}>
        <span className="watch-type">{isAutomatic ? 'AUTOMATIC' : 'SMARTWATCH'}</span>
        <img src={hasPhoto ? imageUrl : (isAutomatic ? automaticPlaceholder : smartPlaceholder)} alt={hasPhoto ? watch.name : `Illustration of ${isAutomatic ? 'an automatic watch' : 'a smartwatch'}`} loading="lazy" decoding="async" width="400" height="440" referrerPolicy="no-referrer" onError={() => { if (hasPhoto) setFailedImage(imageUrl); }} />
        {!hasPhoto && <span className="placeholder-label">Illustration</span>}
        {!inStock && <span className="out-of-stock">Out of Stock</span>}
      </div>
      <div className="watch-info">
        <p className="watch-brand">{watch.brand}</p>
        <h3>{watch.name}</h3>
        <p className="watch-model">{watch.modelNumber}</p>
        <div className="watch-card-bottom">
          <span className="watch-price">{formatPrice(watch.price)}</span>
          <span className={`stock-status ${inStock ? '' : 'stock-empty'}`}><span />{inStock ? `${watch.stockQuantity} in stock` : 'Out of Stock'}</span>
        </div>
        <button className="button add-to-cart" type="button" aria-label={`Add ${watch.name} to Shopping Cart`} disabled={busy || status !== 'ready' || !inStock || Boolean(user && user.role !== 'CUSTOMER')} onClick={addToCart}><Icon name="bag" size={16} />{busy ? 'Adding...' : !inStock ? 'Out of Stock' : user && user.role !== 'CUSTOMER' ? 'Customers Only' : 'Add to Cart'}</button>
        {feedback && <p className="card-feedback" role="status">{feedback} <Link to="/cart">View Shopping Cart →</Link></p>}
        {error && <p className="card-error" role="alert">{error}</p>}
      </div>
    </article>
  );
}
