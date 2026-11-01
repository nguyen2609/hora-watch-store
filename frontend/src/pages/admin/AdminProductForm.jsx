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
      if (!form[name].trim()) invalid[name] = 'Vui lòng nhập thông tin này.';
    }
    if (!categories.data?.some(category => String(category.id) === form.categoryId)) invalid.categoryId = 'Vui lòng chọn danh mục hiện có.';
    if (!/^(0|[1-9][0-9]{0,11})$/.test(form.price)) invalid.price = 'Giá phải là số nguyên từ 0 đến 999999999999 VND.';
    if (!/^(0|[1-9][0-9]{0,9})$/.test(form.stockQuantity) || Number(form.stockQuantity) > 2147483647) invalid.stockQuantity = 'Tồn kho phải là số nguyên từ 0 đến 2147483647.';
    if (form.watchType === 'SMART' && !isOrderId(form.batteryLifeHours)) invalid.batteryLifeHours = 'Thời lượng pin phải là số giờ nguyên lớn hơn 0.';
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
    if (Object.keys(body).length === 0) { setMessage('Thông tin chưa thay đổi.'); return; }
    pending.current = true; setBusy(true);
    try {
      const saved = await request(watch ? `/api/admin/watches/${watch.id}` : '/api/admin/watches', { method: watch ? 'PATCH' : 'POST', body });
      if (!watch) navigate(`/admin/products/${saved.id}/edit`, { replace: true, state: { notice: `Đã tạo sản phẩm #${saved.id}.` } });
      else { const values = fieldsOf(saved); setForm(values); setOriginal(values); setMessage(`Đã cập nhật sản phẩm #${saved.id}.`); }
    } catch (failure) {
      const unknown = !failure.status || failure.status >= 500;
      setUncertain(unknown);
      setError(unknown ? 'Chưa xác định được kết quả lưu. Kiểm tra danh sách hoặc tải lại trang trước khi gửi tiếp.' : errorMessage(failure, 'admin'));
    } finally { pending.current = false; setBusy(false); }
  }
  return <section><Link className="back-to-orders" to="/admin/products">← Danh sách sản phẩm</Link><div className="admin-heading"><div><span className="eyebrow">DETAILS MAKE THE DIFFERENCE</span><h2>{watch ? `Chỉnh sửa sản phẩm #${watch.id}` : 'Thêm sản phẩm'}</h2><p>{watch ? 'Loại đồng hồ được giữ theo sản phẩm đã tạo.' : 'Tạo một thiết kế Automatic hoặc Smartwatch.'}</p></div></div>
    {message && <Notice variant="success">{message}</Notice>}{error && <Notice>{error}{uncertain && <button onClick={() => window.location.reload()}>Tải lại trang</button>}</Notice>}
    <AdminLoadState {...categories} />
    {categories.data && <form className="admin-form" onSubmit={submit} noValidate aria-busy={busy}>
      <div className="admin-form-grid">
        <div className="form-field"><label htmlFor="product-categoryId">Danh mục</label><select id="product-categoryId" value={form.categoryId} onChange={event => change('categoryId', event.target.value)} disabled={locked} aria-invalid={Boolean(errors.categoryId)} aria-describedby={errors.categoryId ? 'category-error' : undefined}><option value="">Chọn danh mục</option>{categories.data.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select>{errors.categoryId && <small className="field-error" id="category-error">{errors.categoryId}</small>}</div>
        <div className="form-field"><label htmlFor="product-watchType">Loại đồng hồ</label><select id="product-watchType" value={form.watchType} onChange={event => change('watchType', event.target.value)} disabled={locked || Boolean(watch)}><option value="AUTOMATIC">Automatic</option><option value="SMART">Smartwatch</option></select></div>
        {input('name', 'Tên sản phẩm', { required: true, maxLength: 150 })}
        {input('brand', 'Thương hiệu', { required: true, maxLength: 100 })}
        {input('modelNumber', 'Mã mẫu', { required: true, maxLength: 100 })}
        {input('price', 'Giá bán (VND)', { required: true, inputMode: 'numeric' })}
        {input('stockQuantity', 'Tồn kho', { required: true, inputMode: 'numeric' })}
        {form.watchType === 'AUTOMATIC' ? input('movementType', 'Bộ máy', { required: true, maxLength: 100 }) : input('batteryLifeHours', 'Thời lượng pin (giờ)', { required: true, inputMode: 'numeric' })}
      </div>
      {input('imageUrl', 'Đường dẫn hình ảnh', { type: 'url' })}
      <div className="form-field"><label htmlFor="product-description">Mô tả</label><textarea id="product-description" value={form.description} onChange={event => change('description', event.target.value)} rows={4} disabled={locked} /></div>
      <label className="admin-checkbox"><input id="product-available" type="checkbox" checked={form.available} onChange={event => change('available', event.target.checked)} disabled={locked} /> Đang bán</label>
      {categories.data.length === 0 && <Notice>Chưa có danh mục. Cần có danh mục trước khi thêm sản phẩm.</Notice>}
      <div className="page-actions"><button className="button button-dark" type="submit" disabled={locked || !categories.data.length}>{busy ? 'Đang lưu...' : watch ? 'Lưu thay đổi' : 'Tạo sản phẩm'}</button><Link className="button button-outline" to="/admin/products">Về danh sách</Link></div>
    </form>}
  </section>;
}

function ExistingProduct({ id }) {
  const source = useAdminData(`/api/admin/watches/${id}`);
  return <><AdminLoadState {...source} />{source.data && <ProductEditor key={source.data.id} watch={source.data} />}</>;
}

export default function AdminProductForm() {
  const { id } = useParams();
  if (id && !isOrderId(id)) return <Notice>Mã sản phẩm không hợp lệ.</Notice>;
  return id ? <ExistingProduct key={id} id={id} /> : <ProductEditor key="new" />;
}
