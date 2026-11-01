export function isOrderId(value) {
  return /^[1-9][0-9]{0,9}$/.test(value) && Number(value) <= 2147483647;
}

export function loginDestination(value) {
  if (['/', '/#collection', '/cart', '/checkout', '/orders', '/admin', '/admin/products', '/admin/products/new', '/admin/orders'].includes(value)) return value;
  const adminMatch = typeof value === 'string' && value.match(/^\/admin\/(?:orders\/([^/]+)|products\/([^/]+)\/edit)$/);
  if (adminMatch && isOrderId(adminMatch[1] || adminMatch[2])) return value;
  const match = typeof value === 'string' && value.match(/^\/orders\/([^/]+)$/);
  return match && isOrderId(match[1]) ? value : '/';
}
