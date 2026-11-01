import Notice from './Notice.jsx';

export default function AdminLoadState({ loading, error, reload }) {
  if (loading) return <div className="admin-loading" role="status">Đang tải dữ liệu quản trị...</div>;
  if (error) return <Notice>{error} <button onClick={reload}>Tải lại dữ liệu</button></Notice>;
  return null;
}
