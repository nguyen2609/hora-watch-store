import Icon from './Icon.jsx';
import heroWatch from '../assets/hero-watch.svg';

export default function Hero() {
  return (
    <section className="hero container" aria-labelledby="hero-heading">
      <div className="hero-copy">
        <span className="eyebrow"><span className="tiny-line" /> THE ART OF TIME</span>
        <h1 id="hero-heading">Time,<br />on your<br /><em>terms.</em></h1>
        <p>A watch does more than tell time.<br className="desktop-break" /> It tells the story of your style.</p>
        <a className="button button-dark" href="#collection">Explore Products <Icon name="arrow" size={19} /></a>
        <div className="hero-note"><span className="note-line" /> REFINEMENT IN EVERY MOVEMENT</div>
      </div>
      <div className="hero-visual">
        <span className="hero-orbit orbit-one" /><span className="hero-orbit orbit-two" />
        <span className="hero-coordinate">H / 01</span>
        <img className="hero-watch" src={heroWatch} alt="Illustration of an automatic watch with a cream dial, bronze case and brown leather strap" width="640" height="800" fetchPriority="high" />
        <div className="hero-caption"><span className="caption-star">✳</span><span>MECHANICAL BEAUTY<small>Timeless beyond the trends.</small></span></div>
        <span className="hero-image-note">Illustration</span>
      </div>
    </section>
  );
}
