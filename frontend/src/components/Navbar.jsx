import Icon from './Icon.jsx';

export default function Navbar({ onTypeChange }) {
  return (
    <>
      <div className="announcement">DẤU ẤN RIÊNG, TRONG TỪNG KHOẢNH KHẮC</div>
      <header className="site-header">
        <nav className="navbar container" aria-label="Điều hướng chính">
          <a className="brand" href="#home" aria-label="HORA — Trang chủ">
            <span className="brand-mark"><Icon name="clock" size={28} /></span>
            <span>HORA<span className="brand-caption">WATCH STORE</span></span>
          </a>
          <div className="nav-links">
            <a href="#collection" onClick={() => onTypeChange('ALL')}>Bộ sưu tập</a>
            <a href="#collection" onClick={() => onTypeChange('AUTOMATIC')}>Automatic</a>
            <a href="#collection" onClick={() => onTypeChange('SMART')}>Smartwatch</a>
          </div>
          <a className="nav-cta" href="#collection">Khám phá <Icon name="arrow" size={17} /></a>
        </nav>
      </header>
    </>
  );
}
