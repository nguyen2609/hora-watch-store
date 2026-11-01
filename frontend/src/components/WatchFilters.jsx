import Icon from './Icon.jsx';

const types = [
  { value: 'ALL', label: 'Tất cả đồng hồ' },
  { value: 'AUTOMATIC', label: 'Automatic' },
  { value: 'SMART', label: 'Smartwatch' },
];

export default function WatchFilters({ filters, onFiltersChange, watchType, onTypeChange, onApply, onReset, hasFilters }) {
  return (
    <div className="filters">
      <form className="filter-form" onSubmit={onApply} aria-label="Tìm kiếm và lọc đồng hồ">
        <div className="search-field">
          <label htmlFor="keyword" className="sr-only">Tên đồng hồ hoặc thương hiệu</label>
          <Icon name="search" size={21} />
          <input id="keyword" name="keyword" type="search" maxLength="150" placeholder="Tìm tên đồng hồ, thương hiệu..." value={filters.keyword} onChange={(event) => onFiltersChange({ ...filters, keyword: event.target.value })} />
        </div>
        <div className="price-field">
          <label htmlFor="max-price">Giá tối đa <span>(₫)</span></label>
          <input id="max-price" name="maxPrice" type="number" min="0" max="999999999999" step="1" inputMode="numeric" placeholder="Không giới hạn" value={filters.maxPrice} onChange={(event) => onFiltersChange({ ...filters, maxPrice: event.target.value })} />
        </div>
        <button className="button filter-submit" type="submit"><Icon name="sliders" size={18} /> Áp dụng</button>
      </form>
      <div className="filter-bottom">
        <div className="type-filters" role="group" aria-label="Loại đồng hồ">
          {types.map((type) => (
            <button key={type.value} type="button" className={`type-filter ${watchType === type.value ? 'is-active' : ''}`} aria-pressed={watchType === type.value} onClick={() => onTypeChange(type.value)}>{type.label}</button>
          ))}
        </div>
        {hasFilters && <button className="reset-filters" type="button" onClick={onReset}><Icon name="refresh" size={14} /> Xóa bộ lọc</button>}
      </div>
    </div>
  );
}
