export function isOrderId(value) {
  return /^[1-9][0-9]{0,9}$/.test(value) && Number(value) <= 2147483647;
}

export function loginDestination(value) {
  if (['/', '/#collection', '/cart', '/checkout', '/orders'].includes(value)) return value;
  const match = typeof value === 'string' && value.match(/^\/orders\/([^/]+)$/);
  return match && isOrderId(match[1]) ? value : '/';
}
