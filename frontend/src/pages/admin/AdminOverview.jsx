import { Link } from 'react-router';
import useAdminData from '../../hooks/useAdminData.js';
import AdminLoadState from '../../components/AdminLoadState.jsx';

export default function AdminOverview() {
  const products = useAdminData('/api/admin/watches');
  const orders = useAdminData('/api/admin/orders');
  return <section><div className="admin-heading"><div><span className="eyebrow">THE BIG PICTURE</span><h2>Tổng quan cửa hàng</h2><p>Sản phẩm, đơn hàng và thanh toán trong cùng một không gian.</p></div></div>
    <AdminLoadState {...products} /><AdminLoadState {...orders} />
    {products.data && orders.data && <><div className="admin-stats"><article><span>Tổng sản phẩm</span><strong>{products.data.length}</strong><small>{products.data.filter(watch => watch.available).length} đang bán</small></article><article><span>Đơn chờ xác nhận</span><strong>{orders.data.filter(order => order.status === 'PENDING').length}</strong><small>{orders.data.length} đơn trong cửa hàng</small></article><article><span>COD cần xác nhận</span><strong>{orders.data.filter(order => order.status === 'DELIVERED' && order.payment?.status === 'UNPAID').length}</strong><small>Đơn đã giao, chưa thanh toán</small></article></div><div className="admin-quick-links"><Link to="/admin/products"><span>01 / COLLECTION</span><h3>Quản lý sản phẩm ↗</h3><p>Cập nhật thông tin, giá và tồn kho của bộ sưu tập.</p></Link><Link to="/admin/orders"><span>02 / OPERATIONS</span><h3>Đơn hàng & thanh toán ↗</h3><p>Theo dõi giao hàng và xác nhận thu tiền COD.</p></Link></div></>}
  </section>;
}
