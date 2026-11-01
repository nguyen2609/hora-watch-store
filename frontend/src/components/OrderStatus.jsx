const labels = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

export function paymentStatus(status) {
  return ({ UNPAID: 'Chưa thanh toán', PAID: 'Đã thanh toán' })[status] || status;
}

export default function OrderStatus({ status }) {
  return <span className={`order-status status-${labels[status] ? status.toLowerCase() : 'unknown'}`}>{labels[status] || status}</span>;
}
