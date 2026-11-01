const labels = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  SHIPPING: 'Shipping',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

export function paymentStatus(status) {
  return ({ UNPAID: 'Unpaid', PAID: 'Paid' })[status] || status;
}

export default function OrderStatus({ status }) {
  return <span className={`order-status status-${labels[status] ? status.toLowerCase() : 'unknown'}`}>{labels[status] || status}</span>;
}
