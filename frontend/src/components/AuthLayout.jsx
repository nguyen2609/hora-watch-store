import { Link } from 'react-router';
import heroWatch from '../assets/hero-watch.svg';

export default function AuthLayout({ eyebrow, title, description, children }) {
  return (
    <section className="auth-page container">
      <aside className="auth-art" aria-label="Phong cách HORA">
        <span className="eyebrow">A MOMENT OF YOUR OWN</span>
        <h2>Mỗi khoảnh khắc,<br /><em>một dấu ấn riêng.</em></h2>
        <img src={heroWatch} alt="Hình minh họa đồng hồ HORA dây da nâu" width="640" height="800" />
        <Link to="/#collection">Khám phá bộ sưu tập →</Link>
      </aside>
      <div className="auth-form-panel">
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p className="auth-description">{description}</p>
        {children}
      </div>
    </section>
  );
}
