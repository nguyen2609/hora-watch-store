import { Link } from 'react-router';
import heroWatch from '../assets/hero-watch.svg';

export default function AuthLayout({ eyebrow, title, description, children }) {
  return (
    <section className="auth-page container">
      <aside className="auth-art" aria-label="The HORA style">
        <span className="eyebrow">A MOMENT OF YOUR OWN</span>
        <h2>Every moment,<br /><em>your own signature.</em></h2>
        <img src={heroWatch} alt="Illustration of a HORA watch with a brown leather strap" width="640" height="800" />
        <Link to="/#collection">Explore Products →</Link>
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
