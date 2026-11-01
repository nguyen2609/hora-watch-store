export const formatPrice = (value) => new Intl.NumberFormat('en-GB', {
  style: 'currency', currency: 'VND', currencyDisplay: 'code', maximumFractionDigits: 0,
}).format(value);

export function formatDate(value) {
  // The API returns LocalDateTime without an offset; do not append a UTC suffix.
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Not available';
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(date);
}
