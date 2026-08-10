// Fleetra brand mark.
// The glyph is a stylised route: a winding path climbing to a glowing,
// pulsing endpoint — echoing the "live tracking" dot used across the app.
export default function Logo({ size = 36, showText = true, variant = 'dark' }) {
  // 'light' variant sits on the always-dark navy sidebar (Driver layout,
  // untouched by dark mode) so it stays plain white. 'dark' variant sits on
  // the theme-orange sidebar (Admin/Dispatcher/Customer + the public
  // tracking header), whose background flips from white to dark navy in
  // dark mode (see index.css [data-theme="dark"] .theme-orange) — so its
  // text must follow the same --sidebar-text variable the sidebar itself
  // uses, or the logo goes near-black-on-dark-navy and disappears.
  const textColor = variant === 'light' ? '#ffffff' : 'var(--sidebar-text, #111827)';
  const subColor = '#fe8704';

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <svg width={size} height={size} viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="fleetraGrad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
            <stop stopColor="#ffab40" />
            <stop offset="1" stopColor="#fe8704" />
          </linearGradient>
        </defs>
        <rect width="48" height="48" rx="13" fill="url(#fleetraGrad)" />
        {/* winding route */}
        <path
          d="M8 37c5 0 5-8 10-8s5 8 10 8 5-16 10-16"
          stroke="#ffffff"
          strokeWidth="3"
          strokeLinecap="round"
          fill="none"
        />
        {/* origin node */}
        <circle cx="8" cy="37" r="2.6" fill="#ffffff" />
        {/* live/destination node, pulsing */}
        <circle cx="38" cy="13" r="3.4" fill="#ffffff" className="animate-pulse-dot" />
        <circle cx="38" cy="13" r="3.4" fill="none" stroke="#ffffff" strokeOpacity="0.5" strokeWidth="1.2" />
      </svg>
      {showText && (
        <span
          style={{
            fontFamily: "'Poppins', sans-serif",
            fontWeight: 800,
            fontSize: size * 0.46,
            color: textColor,
            letterSpacing: '-0.5px',
            lineHeight: 1,
          }}
        >
          Fleet<span style={{ color: subColor }}>ra</span>
        </span>
      )}
    </div>
  );
}
