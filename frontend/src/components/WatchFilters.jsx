import Icon from './Icon.jsx';

const types = [
  { value: 'ALL', label: 'All Products' },
  { value: 'AUTOMATIC', label: 'Automatic' },
  { value: 'SMART', label: 'Smartwatch' },
];

export default function WatchFilters({ filters, onFiltersChange, watchType, onTypeChange, onApply, onReset, hasFilters }) {
  return (
    <div className="filters">
      <form className="filter-form" onSubmit={onApply} aria-label="Search and filter products">
        <div className="search-field">
          <label htmlFor="keyword" className="sr-only">Product name or brand</label>
          <Icon name="search" size={21} />
          <input id="keyword" name="keyword" type="search" maxLength="150" placeholder="Search products or brands..." value={filters.keyword} onChange={(event) => onFiltersChange({ ...filters, keyword: event.target.value })} />
        </div>
        <div className="price-field">
          <label htmlFor="max-price">Maximum Price <span>(VND)</span></label>
          <input id="max-price" name="maxPrice" type="number" min="0" max="999999999999" step="1" inputMode="numeric" placeholder="No limit" value={filters.maxPrice} onChange={(event) => onFiltersChange({ ...filters, maxPrice: event.target.value })} />
        </div>
        <button className="button filter-submit" type="submit"><Icon name="sliders" size={18} /> Apply</button>
      </form>
      <div className="filter-bottom">
        <div className="type-filters" role="group" aria-label="Watch Type">
          {types.map((type) => (
            <button key={type.value} type="button" className={`type-filter ${watchType === type.value ? 'is-active' : ''}`} aria-pressed={watchType === type.value} onClick={() => onTypeChange(type.value)}>{type.label}</button>
          ))}
        </div>
        {hasFilters && <button className="reset-filters" type="button" onClick={onReset}><Icon name="refresh" size={14} /> Clear Filters</button>}
      </div>
    </div>
  );
}
