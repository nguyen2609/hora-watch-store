import { useEffect, useState } from 'react';
import Navbar from './components/Navbar.jsx';
import Hero from './components/Hero.jsx';
import WatchCard, { formatPrice } from './components/WatchCard.jsx';
import WatchFilters from './components/WatchFilters.jsx';
import Icon from './components/Icon.jsx';

const emptyFilters = { keyword: '', maxPrice: '' };

export default function App() {
  const [filters, setFilters] = useState(emptyFilters);
  const [query, setQuery] = useState(emptyFilters);
  const [watchType, setWatchType] = useState('ALL');
  const [watches, setWatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function loadWatches() {
      setLoading(true);
      setError('');
      const params = new URLSearchParams();
      if (query.keyword) params.set('keyword', query.keyword);
      if (query.maxPrice !== '') params.set('maxPrice', query.maxPrice);

      try {
        const response = await fetch(`/api/watches${params.size ? `?${params}` : ''}`, {
          signal: controller.signal,
          headers: { Accept: 'application/json' },
          credentials: 'same-origin',
        });
        if (!response.ok) throw new Error('Catalog request failed.');
        const data = await response.json();
        if (!Array.isArray(data)) throw new Error('Unexpected catalog response.');
        if (active) setWatches(data);
      } catch (failure) {
        if (active && failure.name !== 'AbortError') {
          setWatches([]);
          setError('Chưa thể tải bộ sưu tập. Vui lòng thử lại hoặc kiểm tra API Java tại cổng 8080.');
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    loadWatches();
    // Cancel an older request so it cannot overwrite newer search results.
    return () => { active = false; controller.abort(); };
  }, [query.keyword, query.maxPrice, retry]);

  // The API supports keyword/maxPrice. Type filtering uses the returned watchType field.
  const visibleWatches = watches.filter((watch) => watchType === 'ALL' || watch.watchType === watchType);
  const hasFilters = Boolean(filters.keyword || filters.maxPrice || query.keyword || query.maxPrice || watchType !== 'ALL');

  function applyFilters(event) {
    event.preventDefault();
    setQuery({ keyword: filters.keyword.trim(), maxPrice: filters.maxPrice });
  }

  function resetFilters() {
    setFilters(emptyFilters);
    setQuery(emptyFilters);
    setWatchType('ALL');
  }

  return (
    <div id="home">
      <a className="skip-link" href="#collection">Đến bộ sưu tập</a>
      <Navbar onTypeChange={setWatchType} />
      <main>
        <Hero />
        <div className="values-strip container" aria-label="Khám phá HORA">
          <div><Icon name="clock" size={26} /><span>Hai thế giới thời gian<small>Automatic & Smartwatch</small></span></div>
          <div><Icon name="sparkle" size={26} /><span>Dấu ấn của riêng bạn<small>Từ cổ điển đến hiện đại</small></span></div>
          <div><Icon name="check" size={26} /><span>Giá niêm yết rõ ràng<small>Hiển thị bằng Việt Nam đồng</small></span></div>
        </div>

        <section id="collection" className="collection container" aria-labelledby="collection-heading">
          <div className="section-heading">
            <div><span className="eyebrow">TÌM CHIẾC ĐỒNG HỒ CỦA BẠN</span><h2 id="collection-heading">Chọn một dấu ấn riêng.</h2></div>
            <p>Vẻ đẹp cơ khí hay nhịp sống thông minh?<br />Khám phá thiết kế đồng hành cùng bạn.</p>
          </div>
          <WatchFilters filters={filters} onFiltersChange={setFilters} watchType={watchType} onTypeChange={setWatchType} onApply={applyFilters} onReset={resetFilters} hasFilters={hasFilters} />

          <div className="results-heading" role="status" aria-live="polite">
            <span>{loading ? 'Đang tìm những thiết kế dành cho bạn...' : error ? 'Bộ sưu tập chưa khả dụng' : <><strong>{visibleWatches.length}</strong> thiết kế trong bộ sưu tập</>}</span>
            <span className="applied-query">{query.keyword && `“${query.keyword}”`}{query.keyword && query.maxPrice !== '' && ' · '}{query.maxPrice !== '' && `Đến ${formatPrice(query.maxPrice)}`}</span>
          </div>

          <div aria-busy={loading} className="catalog-content">
            {loading ? (
              <div className="watch-grid" aria-label="Đang tải đồng hồ">
                {Array.from({ length: 4 }, (_, index) => <div className="skeleton-card" key={index}><div className="skeleton-image" /><div className="skeleton-line" /><div className="skeleton-line short" /></div>)}
              </div>
            ) : error ? (
              <div className="catalog-state" role="alert"><span className="state-icon"><Icon name="refresh" size={30} /></span><h3>Một nhịp nghỉ nhỏ.</h3><p>{error}</p><button className="button button-dark" onClick={() => setRetry((value) => value + 1)}>Thử lại <Icon name="refresh" size={16} /></button></div>
            ) : visibleWatches.length === 0 ? (
              <div className="catalog-state"><span className="state-icon"><Icon name="search" size={30} /></span><h3>Chưa tìm thấy thiết kế phù hợp.</h3><p>Thử một tên khác, tăng mức giá hoặc khám phá toàn bộ bộ sưu tập.</p><button className="button button-dark" onClick={resetFilters}>Xem tất cả đồng hồ <Icon name="arrow" size={17} /></button></div>
            ) : (
              <div className="watch-grid">{visibleWatches.map((watch) => <WatchCard key={watch.id} watch={watch} />)}</div>
            )}
          </div>
        </section>

        <section className="closing-note container" aria-label="Phong cách HORA"><span className="eyebrow">MORE THAN A TIMEPIECE</span><p>Không chỉ là thời gian.<br />Là cách bạn <em>để lại dấu ấn.</em></p><a href="#collection">Tìm thiết kế của bạn <Icon name="arrow" size={18} /></a></section>
      </main>
      <footer className="site-footer"><div className="container footer-inner"><a className="footer-brand" href="#home">HORA<span>WATCH STORE</span></a><p>Một chiếc đồng hồ. Một câu chuyện riêng.</p><span>© {new Date().getFullYear()} HORA</span></div></footer>
    </div>
  );
}
