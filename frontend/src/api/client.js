export class ApiError extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

// Cookies stay in the browser. Only the separate CSRF header is read by JavaScript.
export async function apiRequest(path, { method = 'GET', body, csrfToken, signal } = {}) {
  const headers = { Accept: 'application/json' };
  if (method !== 'GET') headers['X-Requested-With'] = 'WatchStore';
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (csrfToken && method !== 'GET') headers['X-CSRF-Token'] = csrfToken;

  let response;
  try {
    response = await fetch(path, {
      method, headers, credentials: 'same-origin', signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError(0, 'NETWORK_ERROR');
  }

  let data = null;
  if (response.status !== 204) {
    try { data = await response.json(); } catch {
      throw new ApiError(response.status >= 400 ? response.status : 502, 'INVALID_RESPONSE');
    }
  }
  if (!response.ok) throw new ApiError(response.status, data?.code || 'REQUEST_FAILED');
  return { data, csrfToken: response.headers.get('X-CSRF-Token') };
}

export function errorMessage(error, action = 'request') {
  if (error.code === 'INVALID_CREDENTIALS') return 'Incorrect email address or password.';
  if (error.code === 'EMAIL_EXISTS') return 'This email address is already registered. Log in or use a different address.';
  if (error.code === 'CSRF_REJECTED') return 'Your security session has changed. Please try the action again.';
  if (error.code === 'SESSION_CHANGED') return 'Your session has changed. Please reload the page.';
  if (error.status === 401) return 'Your session has expired. Please log in again.';
  if (error.status === 403) return 'Your account or request origin is not authorized to perform this action.';
  if (action === 'admin' && error.status === 404) return 'The requested admin data was not found. Please reload the list.';
  if (action === 'admin' && error.status === 400) return 'Invalid information. Check the category, unique model number, nonnegative whole-number price and stock quantity.';
  if (action === 'admin' && error.status === 409) return 'This action conflicts with the current status, or the model number is already in use. Reload the data before trying again.';
  if (error.status === 404 && action === 'orders') return 'This order was not found or you do not have permission to view it.';
  if (error.status === 404 && action === 'checkout') return 'A product is no longer available. Please review your shopping cart.';
  if (error.status === 404) return 'The product was not found or is no longer available. Please reload your shopping cart.';
  if (error.status === 400 && action === 'cart') return 'Unable to update your shopping cart. Check that the quantity is valid, enough stock is available and the product is still on sale. Your shopping cart has not changed.';
  if (error.status === 400 && action === 'checkout') return 'Unable to place your order. Please check stock, quantities and shipping details. No order was created.';
  if (error.status === 409 && action === 'checkout') return 'Your shopping cart is empty or has changed. Please review it before placing your order again.';
  if (error.status === 400) return 'Invalid information. Please check the fields you entered.';
  if (error.status === 409) return 'The data has changed or this action was already performed. Please review it before trying again.';
  return 'Unable to connect to the service. Please try again later. Avoid repeated submissions if the outcome is uncertain.';
}
