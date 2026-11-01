import { useRef, useState } from 'react';
import { Link } from 'react-router';
import { useAuth } from '../../context/AuthContext.jsx';
import useAdminData from '../../hooks/useAdminData.js';
import AdminLoadState from '../../components/AdminLoadState.jsx';
import Notice from '../../components/Notice.jsx';
import { errorMessage } from '../../api/client.js';
import { formatPrice } from '../../utils/format.js';

export default function AdminProducts() {
  const products = useAdminData('/api/admin/watches');
  const { request } = useAuth();
  const [keyword, setKeyword] = useState('');
  const [availability, setAvailability] = useState('ALL');
  const [selected, setSelected] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const pending = useRef(false);
  async function discontinue() {
    if (pending.current || !selected) return;
    pending.current = true;
    setBusy(true); setError(''); setMessage('');
    try {
      await request(`/api/admin/watches/${selected.id}`, { method: 'DELETE' });
      products.setData(current => current.map(watch => watch.id === selected.id ? { ...watch, available: false } : watch));
      setMessage(`Discontinued ${selected.name}. The product remains in the system.`);
      setSelected(null);
    } catch (failure) { setError(errorMessage(failure, 'admin')); }
    finally { pending.current = false; setBusy(false); }
  }
  const visible = (products.data || []).filter(watch => (availability === 'ALL' || watch.available === (availability === 'ACTIVE')) && `${watch.name} ${watch.brand} ${watch.modelNumber}`.toLowerCase().includes(keyword.toLowerCase()));
  return <section><div className="admin-heading"><div><span className="eyebrow">CURATE THE COLLECTION</span><h2>Product Management</h2><p>Includes active and discontinued products.</p></div><Link className="button button-dark" to="/admin/products/new">Add Product</Link></div>
    {message && <Notice variant="success">{message}</Notice>}{error && <Notice>{error} <button disabled={busy} onClick={() => { setSelected(null); products.reload(); }}>Review Product List</button></Notice>}
    {selected && <div className="admin-confirm" role="group" aria-label="Confirm Discontinuation"><p>Discontinue <strong>{selected.name}</strong>?</p><button className="button button-dark" disabled={busy} onClick={discontinue}>{busy ? 'Processing...' : 'Confirm Discontinuation'}</button><button className="button button-outline" disabled={busy} onClick={() => setSelected(null)}>Keep Product</button></div>}
    <div className="admin-filters"><label>Search Products<input id="admin-product-search" type="search" value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Name, brand or model number" /></label><label>Availability<select value={availability} onChange={event => setAvailability(event.target.value)}><option value="ALL">All Products</option><option value="ACTIVE">Active</option><option value="INACTIVE">Discontinued</option></select></label><button className="button button-outline" disabled={busy || products.loading} onClick={products.reload}>Refresh</button></div>
    <AdminLoadState {...products} />
    {products.data && <><p className="admin-count">{visible.length} {visible.length === 1 ? 'product' : 'products'}</p><div className="admin-product-list">{visible.map(watch => <article className="admin-product" key={watch.id} data-product-id={watch.id}><div className="admin-product-monogram" aria-hidden="true">{watch.watchType === 'SMART' ? 'S' : 'A'}</div><div className="admin-product-info"><span className="eyebrow">{watch.brand} · {watch.watchType}</span><h3>{watch.name}</h3><p>{watch.modelNumber} · #{watch.id}</p><span className={`admin-availability ${watch.available ? '' : 'inactive'}`}>{watch.available ? 'Active' : 'Discontinued'}</span></div><div className="admin-product-numbers"><strong>{formatPrice(watch.price)}</strong><span>Stock: {watch.stockQuantity}</span></div><div className="admin-row-actions"><Link to={`/admin/products/${watch.id}/edit`}>Edit ↗</Link>{watch.available && <button disabled={busy} onClick={() => { setSelected(watch); setError(''); }}>Discontinue</button>}</div></article>)}</div>{visible.length === 0 && <div className="admin-empty">No matching products found.</div>}</>}
  </section>;
}
