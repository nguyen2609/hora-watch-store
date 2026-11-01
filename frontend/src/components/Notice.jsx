export default function Notice({ children, variant = 'error' }) {
  return <div className={`notice notice-${variant}`} role={variant === 'error' ? 'alert' : 'status'}>{children}</div>;
}
