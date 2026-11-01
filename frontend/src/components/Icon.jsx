// A small, shared icon component keeps the UI free of an extra icon dependency.
export default function Icon({ name, size = 20, ...props }) {
  const paths = {
    arrow: <><path d="M4 12h15M13 6l6 6-6 6" /></>,
    search: <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4 4" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 6v6l4 3" /></>,
    sliders: <><path d="M4 7h4m4 0h8M4 17h8m4 0h4" /><circle cx="10" cy="7" r="2" /><circle cx="14" cy="17" r="2" /></>,
    sparkle: <><path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z" /></>,
    check: <><path d="m5 12 4 4L19 6" /></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M6.1 6.1A8 8 0 0 1 20 12M4 12a8 8 0 0 0 13.9 5.9" /></>,
  };

  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
      {paths[name] || paths.clock}
    </svg>
  );
}
