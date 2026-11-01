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
  if (error.code === 'INVALID_CREDENTIALS') return 'Email hoặc mật khẩu không đúng.';
  if (error.code === 'EMAIL_EXISTS') return 'Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.';
  if (error.code === 'CSRF_REJECTED') return 'Phiên bảo mật đã thay đổi. Vui lòng thử lại thao tác.';
  if (error.code === 'SESSION_CHANGED') return 'Phiên đăng nhập đã thay đổi. Vui lòng tải lại trang.';
  if (error.status === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
  if (error.status === 403) return 'Tài khoản hoặc nguồn truy cập không được phép thực hiện thao tác này.';
  if (error.status === 404 && action === 'orders') return 'Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn này.';
  if (error.status === 404 && action === 'checkout') return 'Một sản phẩm không còn khả dụng. Hãy quay lại kiểm tra giỏ hàng.';
  if (error.status === 404) return 'Không tìm thấy sản phẩm hoặc sản phẩm không còn khả dụng. Hãy tải lại giỏ hàng.';
  if (error.status === 400 && action === 'cart') return 'Không thể thay đổi giỏ hàng. Số lượng phải hợp lệ, còn đủ tồn kho và sản phẩm vẫn được bán. Giỏ hàng chưa được cập nhật.';
  if (error.status === 400 && action === 'checkout') return 'Không thể đặt đơn. Vui lòng kiểm tra tồn kho, số lượng và thông tin nhận hàng. Đơn chưa được tạo.';
  if (error.status === 409 && action === 'checkout') return 'Giỏ hàng trống hoặc đã thay đổi. Hãy kiểm tra giỏ hàng trước khi đặt lại.';
  if (error.status === 400) return 'Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường đã nhập.';
  if (error.status === 409) return 'Dữ liệu vừa thay đổi hoặc thao tác bị trùng. Vui lòng kiểm tra rồi thử lại.';
  return 'Chưa thể kết nối dịch vụ. Vui lòng thử lại sau; không gửi lại liên tục nếu chưa rõ kết quả.';
}
