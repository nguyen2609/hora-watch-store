import Icon from './Icon.jsx';
import heroWatch from '../assets/hero-watch.svg';

export default function Hero() {
  return (
    <section className="hero container" aria-labelledby="hero-heading">
      <div className="hero-copy">
        <span className="eyebrow"><span className="tiny-line" /> THE ART OF TIME</span>
        <h1 id="hero-heading">Thời gian,<br />theo cách<br /><em>của bạn.</em></h1>
        <p>Một chiếc đồng hồ không chỉ đếm giờ.<br className="desktop-break" /> Nó kể câu chuyện về phong cách của bạn.</p>
        <a className="button button-dark" href="#collection">Khám phá bộ sưu tập <Icon name="arrow" size={19} /></a>
        <div className="hero-note"><span className="note-line" /> TINH TẾ TRONG TỪNG CHUYỂN ĐỘNG</div>
      </div>
      <div className="hero-visual">
        <span className="hero-orbit orbit-one" /><span className="hero-orbit orbit-two" />
        <span className="hero-coordinate">H / 01</span>
        <img className="hero-watch" src={heroWatch} alt="Minh họa đồng hồ cơ mặt kem, vỏ đồng và dây da nâu" width="640" height="800" fetchPriority="high" />
        <div className="hero-caption"><span className="caption-star">✳</span><span>VẺ ĐẸP CƠ KHÍ<small>Vượt qua những mùa xu hướng.</small></span></div>
        <span className="hero-image-note">Hình ảnh minh họa</span>
      </div>
    </section>
  );
}
