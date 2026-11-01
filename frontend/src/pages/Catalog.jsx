import { useEffect, useState } from 'react';
import Hero from '../components/Hero.jsx';
import WatchCard from '../components/WatchCard.jsx';
import { formatPrice } from '../utils/format.js';
import WatchFilters from '../components/WatchFilters.jsx';
import Icon from '../components/Icon.jsx';

const emptyFilters = { keyword: '', maxPrice: '' };

export default function Catalog({ watchType, onTypeChange }) {
  const [filters, setFilters] = useState(emptyFilters);
  const [query, setQuery] = useState(emptyFilters);
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
          setError('Unable to load products. Please try again shortly.');
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
    onTypeChange('ALL');
  }

  return (
    <>
        <Hero />
        <div className="values-strip container" aria-label="Discover HORA">
          <div><Icon name="clock" size={26} /><span>Two worlds of time<small>Automatic & Smartwatch</small></span></div>
          <div><Icon name="sparkle" size={26} /><span>Your signature style<small>From classic to contemporary</small></span></div>
          <div><Icon name="check" size={26} /><span>Clear, transparent pricing<small>All prices in VND</small></span></div>
        </div>

        <section id="collection" className="collection container" aria-labelledby="collection-heading">
          <div className="section-heading">
            <div><span className="eyebrow">FIND YOUR TIMEPIECE</span><h2 id="collection-heading">Find your signature.</h2></div>
            <p>Mechanical elegance or smart living?<br />Discover a timepiece for your every day.</p>
          </div>
          <WatchFilters filters={filters} onFiltersChange={setFilters} watchType={watchType} onTypeChange={onTypeChange} onApply={applyFilters} onReset={resetFilters} hasFilters={hasFilters} />

          <div className="results-heading" role="status" aria-live="polite">
            <span>{loading ? 'Finding your next timepiece...' : error ? 'Products are currently unavailable' : <><strong>{visibleWatches.length}</strong> {visibleWatches.length === 1 ? 'product' : 'products'} in the collection</>}</span>
            <span className="applied-query">{query.keyword && `“${query.keyword}”`}{query.keyword && query.maxPrice !== '' && ' · '}{query.maxPrice !== '' && `Up to ${formatPrice(query.maxPrice)}`}</span>
          </div>

          <div aria-busy={loading} className="catalog-content">
            {loading ? (
              <div className="watch-grid" aria-label="Loading products">
                {Array.from({ length: 4 }, (_, index) => <div className="skeleton-card" key={index}><div className="skeleton-image" /><div className="skeleton-line" /><div className="skeleton-line short" /></div>)}
              </div>
            ) : error ? (
              <div className="catalog-state" role="alert"><span className="state-icon"><Icon name="refresh" size={30} /></span><h3>A brief pause.</h3><p>{error}</p><button className="button button-dark" onClick={() => setRetry((value) => value + 1)}>Try Again <Icon name="refresh" size={16} /></button></div>
            ) : visibleWatches.length === 0 ? (
              <div className="catalog-state"><span className="state-icon"><Icon name="search" size={30} /></span><h3>No matching products found.</h3><p>Try another search, increase your budget or explore all products.</p><button className="button button-dark" onClick={resetFilters}>View All Products <Icon name="arrow" size={17} /></button></div>
            ) : (
              <div className="watch-grid">{visibleWatches.map((watch) => <WatchCard key={watch.id} watch={watch} />)}</div>
            )}
          </div>
        </section>

        <section className="closing-note container" aria-label="The HORA style"><span className="eyebrow">MORE THAN A TIMEPIECE</span><p>More than telling time.<br />A way to <em>make your mark.</em></p><a href="#collection">Find Your Timepiece <Icon name="arrow" size={18} /></a></section>
    </>
  );
}
