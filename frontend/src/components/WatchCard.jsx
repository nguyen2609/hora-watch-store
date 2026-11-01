import { useState } from 'react';
import automaticPlaceholder from '../assets/watch-automatic.svg';
import smartPlaceholder from '../assets/watch-smart.svg';

export const formatPrice = (value) => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0,
}).format(value);

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
  const [failedImage, setFailedImage] = useState(null);
  const imageUrl = safeImageUrl(watch.imageUrl);
  const hasPhoto = imageUrl && failedImage !== imageUrl;
  const isAutomatic = watch.watchType === 'AUTOMATIC';
  const inStock = watch.stockQuantity > 0;

  return (
    <article className="watch-card">
      <div className={`watch-image-wrap ${isAutomatic ? 'automatic-image' : 'smart-image'}`}>
        <span className="watch-type">{isAutomatic ? 'AUTOMATIC' : 'SMARTWATCH'}</span>
        <img src={hasPhoto ? imageUrl : (isAutomatic ? automaticPlaceholder : smartPlaceholder)} alt={hasPhoto ? watch.name : `Hình minh họa ${isAutomatic ? 'đồng hồ cơ' : 'smartwatch'}`} loading="lazy" decoding="async" width="400" height="440" referrerPolicy="no-referrer" onError={() => { if (hasPhoto) setFailedImage(imageUrl); }} />
        {!hasPhoto && <span className="placeholder-label">Ảnh minh họa</span>}
        {!inStock && <span className="out-of-stock">Tạm hết hàng</span>}
      </div>
      <div className="watch-info">
        <p className="watch-brand">{watch.brand}</p>
        <h3>{watch.name}</h3>
        <p className="watch-model">{watch.modelNumber}</p>
        <div className="watch-card-bottom">
          <span className="watch-price">{formatPrice(watch.price)}</span>
          <span className={`stock-status ${inStock ? '' : 'stock-empty'}`}><span />{inStock ? `Còn ${watch.stockQuantity}` : 'Hết hàng'}</span>
        </div>
      </div>
    </article>
  );
}
