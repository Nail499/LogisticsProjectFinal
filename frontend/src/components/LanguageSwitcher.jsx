// Minimalist AZ | RU | EN language switcher.
// `variant="light"` is meant for dark navbars/topbars (landing page, dark
// dashboard chrome); `variant="default"` works on light surfaces (landing
// topbar); `variant="app"` is for the authenticated dashboard topbar
// (DashboardLayout) — it reads the app's own CSS custom properties
// (--surface/--border/--text/--primary-bg, see index.css) via Tailwind
// arbitrary-value classes so it automatically matches whichever theme
// (light/dark, toggled via ThemeToggle) the user is on, instead of a fixed
// Tailwind palette that would clash in dark mode.
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Globe, ChevronDown } from 'lucide-react';
import { SUPPORTED_LANGUAGES } from '../i18n/index.js';

export default function LanguageSwitcher({ variant = 'light' }) {
  const { i18n } = useTranslation();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  const current =
    SUPPORTED_LANGUAGES.find((l) => l.code === i18n.language) || SUPPORTED_LANGUAGES[0];

  useEffect(() => {
    const onClickOutside = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const select = (code) => {
    i18n.changeLanguage(code);
    setOpen(false);
  };

  const isLight = variant === 'light';
  const isApp = variant === 'app';

  return (
    // z-[60] on the wrapper — higher than any sticky header/hero overlay in
    // the app (the landing page header is z-50; ties at equal z-index are
    // resolved by DOM order, and the header comes later in the markup than
    // the topbar switcher, so without this the switcher silently painted
    // BEHIND the header and both the button and its open dropdown became
    // unclickable / looked "transparent" — see LanguageSwitcher usage in
    // Home.jsx topbar, Login/CustomerRegister/ForgotPassword). The dropdown
    // list itself gets an even higher z-index below so it always wins.
    <div className="relative z-[60]" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold tracking-wide transition-colors ${
          isApp
            ? 'border'
            : isLight
              ? 'text-white bg-black/35 hover:bg-black/50 border border-white/30 backdrop-blur-sm'
              : 'text-slate-700 bg-slate-100 hover:bg-slate-200 border border-slate-200'
        }`}
        style={isApp ? { color: 'var(--text)', background: 'var(--surface)', borderColor: 'var(--border)' } : undefined}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <Globe size={14} />
        {current.label}
        <ChevronDown size={13} className={`transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {/* Açılan qutu da variant-a görə mövzuya uyğunlaşır: `light` (tünd
          hero/foto arxa fon üzərində) tünd qutu saxlayır, `default` (ağ
          səhifə fonu — landing topbar) ağ qutu göstərir, `app` (daxili
          panellər) isə --surface/--border CSS dəyişənlərini istifadə edərək
          açıq/tünd rejimə avtomatik uyğunlaşır. */}
      {open && (
        <ul
          role="listbox"
          className={`absolute right-0 z-[70] mt-2 w-40 overflow-hidden rounded-xl border py-1 ${
            isApp ? '' : isLight ? 'border-base-700 bg-base-900 shadow-glow' : 'border-slate-200 bg-white shadow-lg'
          }`}
          style={isApp ? { background: 'var(--surface)', borderColor: 'var(--border)', boxShadow: 'var(--shadow)' } : undefined}
        >
          {SUPPORTED_LANGUAGES.map((lng) => (
            <li key={lng.code}>
              <button
                type="button"
                role="option"
                aria-selected={lng.code === current.code}
                onClick={() => select(lng.code)}
                className={`flex w-full items-center justify-between px-3.5 py-2 text-sm transition-colors ${
                  isApp
                    ? ''
                    : isLight
                      ? (lng.code === current.code ? 'text-neon-green bg-white/5' : 'text-slate-300 hover:bg-white/5')
                      : (lng.code === current.code ? 'text-[#fe8704] bg-[#fff5ea] font-semibold' : 'text-slate-700 hover:bg-slate-50')
                }`}
                style={isApp ? {
                  color: lng.code === current.code ? 'var(--primary)' : 'var(--text)',
                  background: lng.code === current.code ? 'var(--primary-bg)' : 'transparent',
                  fontWeight: lng.code === current.code ? 600 : 400,
                } : undefined}
              >
                <span>{lng.name}</span>
                <span
                  className={`text-[11px] font-semibold ${isApp ? '' : isLight ? 'text-slate-500' : 'text-slate-400'}`}
                  style={isApp ? { color: 'var(--text-muted)' } : undefined}
                >
                  {lng.label}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
