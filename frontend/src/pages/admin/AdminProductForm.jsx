import { useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { useAuth } from '../../context/AuthContext.jsx';
import useAdminData from '../../hooks/useAdminData.js';
import AdminLoadState from '../../components/AdminLoadState.jsx';
import FormField from '../../components/FormField.jsx';
import Notice from '../../components/Notice.jsx';
import { errorMessage } from '../../api/client.js';
import { isOrderId } from '../../utils/navigation.js';

function fieldsOf(watch) {
  return {
    categoryId: String(watch?.categoryId ?? ''), name: watch?.name ?? '', brand: watch?.brand ?? '',
    modelNumber: watch?.modelNumber ?? '', price: String(watch?.price ?? ''),
    stockQuantity: String(watch?.stockQuantity ?? '0'), watchType: watch?.watchType ?? 'AUTOMATIC',
    movementType: watch?.movementType ?? '', batteryLifeHours: String(watch?.batteryLifeHours ?? ''),
    description: watch?.description ?? '', imageUrl: watch?.imageUrl ?? '', available: watch?.available ?? true,
  };
}

function ProductEditor({ watch = null }) {
  const categories = useAdminData('/api/admin/categories');
  const { request } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState(() => fieldsOf(watch));
  const [original, setOriginal] = useState(() => fieldsOf(watch));
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [message, setMessage] = useState(location.state?.notice || '');
  const [busy, setBusy] = useState(false);
  const [uncertain, setUncertain] = useState(false);
  const pending = useRef(false);
  const locked = busy || uncertain;
  function change(name, value) {
    setForm(current => ({ ...current, [name]: value }));
    setErrors(current => ({ ...current, [name]: '' }));
  }
  function input(name, label, extra = {}) {
    return <FormField id={`product-${name}`} label={label} value={form[name]} onChange={event => change(name, event.target.value)} error={errors[name]} disabled={locked} {...extra} />;
  }
  async function submit(event) {
    event.preventDefault();
    if (pending.current || uncertain) return;
    const invalid = {};
    for (const name of ['name', 'brand', 'modelNumber', ...(form.watchType === 'AUTOMATIC' ? ['movementType'] : [])]) {
      if (!form[name].trim()) invalid[name] = 'Please complete this field.';
    }
    if (!categories.data?.some(category => String(category.id) === form.categoryId)) invalid.categoryId = 'Please select an existing category.';
    if (!/^(0|[1-9][0-9]{0,11})$/.test(form.price)) invalid.price = 'Price must be a whole number from 0 to 999999999999 VND.';
    if (!/^(0|[1-9][0-9]{0,9})$/.test(form.stockQuantity) || Number(form.stockQuantity) > 2147483647) invalid.stockQuantity = 'Stock must be a whole number from 0 to 2147483647.';
    if (form.watchType === 'SMART' && !isOrderId(form.batteryLifeHours)) invalid.batteryLifeHours = 'Battery life must be a positive whole number of hours.';
    setErrors(invalid); setError(''); setMessage('');
    if (Object.keys(invalid).length) return;
    const names = ['categoryId', 'name', 'brand', 'modelNumber', 'price', 'stockQuantity', 'description', 'imageUrl', 'available', form.watchType === 'SMART' ? 'batteryLifeHours' : 'movementType'];
    const body = {};
    for (const name of names) {
      // Send only edited fields, so an unrelated edit cannot overwrite newer server stock.
      if (watch && form[name] === original[name]) continue;
      body[name] = ['categoryId', 'price', 'stockQuantity', 'batteryLifeHours'].includes(name) ? Number(form[name])
        : ['description', 'imageUrl'].includes(name) ? form[name].trim() || null
          : typeof form[name] === 'string' ? form[name].trim() : form[name];
    }
    if (!watch) body.watchType = form.watchType;
    if (Object.keys(body).length === 0) { setMessage('No changes to save.'); return; }
    pending.current = true; setBusy(true);
    try {
      const saved = await request(watch ? `/api/admin/watches/${watch.id}` : '/api/admin/watches', { method: watch ? 'PATCH' : 'POST', body });
      if (!watch) navigate(`/admin/products/${saved.id}/edit`, { replace: true, state: { notice: `Created product #${saved.id}.` } });
      else { const values = fieldsOf(saved); setForm(values); setOriginal(values); setMessage(`Updated product #${saved.id}.`); }
    } catch (failure) {
      const unknown = !failure.status || failure.status >= 500;
      setUncertain(unknown);
      setError(unknown ? 'The save outcome is uncertain. Check the product list or reload this page before submitting again.' : errorMessage(failure, 'admin'));
    } finally { pending.current = false; setBusy(false); }
  }
  return <section><Link className="back-to-orders" to="/admin/products">← Product List</Link><div className="admin-heading"><div><span className="eyebrow">DETAILS MAKE THE DIFFERENCE</span><h2>{watch ? `Edit Product #${watch.id}` : 'Add Product'}</h2><p>{watch ? 'The watch type cannot be changed after creation.' : 'Create an automatic watch or smartwatch.'}</p></div></div>
    {message && <Notice variant="success">{message}</Notice>}{error && <Notice>{error}{uncertain && <button onClick={() => window.location.reload()}>Reload Page</button>}</Notice>}
    <AdminLoadState {...categories} />
    {categories.data && <form className="admin-form" onSubmit={submit} noValidate aria-busy={busy}>
      <div className="admin-form-grid">
        <div className="form-field"><label htmlFor="product-categoryId">Category</label><select id="product-categoryId" value={form.categoryId} onChange={event => change('categoryId', event.target.value)} disabled={locked} aria-invalid={Boolean(errors.categoryId)} aria-describedby={errors.categoryId ? 'category-error' : undefined}><option value="">Select a category</option>{categories.data.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select>{errors.categoryId && <small className="field-error" id="category-error">{errors.categoryId}</small>}</div>
        <div className="form-field"><label htmlFor="product-watchType">Watch Type</label><select id="product-watchType" value={form.watchType} onChange={event => change('watchType', event.target.value)} disabled={locked || Boolean(watch)}><option value="AUTOMATIC">Automatic</option><option value="SMART">Smartwatch</option></select></div>
        {input('name', 'Product Name', { required: true, maxLength: 150 })}
        {input('brand', 'Brand', { required: true, maxLength: 100 })}
        {input('modelNumber', 'Model Number', { required: true, maxLength: 100 })}
        {input('price', 'Price (VND)', { required: true, inputMode: 'numeric' })}
        {input('stockQuantity', 'Stock', { required: true, inputMode: 'numeric' })}
        {form.watchType === 'AUTOMATIC' ? input('movementType', 'Movement Type', { required: true, maxLength: 100 }) : input('batteryLifeHours', 'Battery Life (hours)', { required: true, inputMode: 'numeric' })}
      </div>
      {input('imageUrl', 'Image URL', { type: 'url' })}
      <div className="form-field"><label htmlFor="product-description">Description</label><textarea id="product-description" value={form.description} onChange={event => change('description', event.target.value)} rows={4} disabled={locked} /></div>
      <label className="admin-checkbox"><input id="product-available" type="checkbox" checked={form.available} onChange={event => change('available', event.target.checked)} disabled={locked} /> Active</label>
      {categories.data.length === 0 && <Notice>No categories are available. A category is required before adding a product.</Notice>}
      <div className="page-actions"><button className="button button-dark" type="submit" disabled={locked || !categories.data.length}>{busy ? 'Saving...' : watch ? 'Save Changes' : 'Create Product'}</button><Link className="button button-outline" to="/admin/products">Back to Products</Link></div>
    </form>}
  </section>;
}

function ExistingProduct({ id }) {
  const source = useAdminData(`/api/admin/watches/${id}`);
  return <><AdminLoadState {...source} />{source.data && <ProductEditor key={source.data.id} watch={source.data} />}</>;
}

export default function AdminProductForm() {
  const { id } = useParams();
  if (id && !isOrderId(id)) return <Notice>Invalid product ID.</Notice>;
  return id ? <ExistingProduct key={id} id={id} /> : <ProductEditor key="new" />;
}
