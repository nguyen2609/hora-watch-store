import Notice from './Notice.jsx';

export default function AdminLoadState({ loading, error, reload }) {
  if (loading) return <div className="admin-loading" role="status">Loading admin data...</div>;
  if (error) return <Notice>{error} <button onClick={reload}>Reload Data</button></Notice>;
  return null;
}
