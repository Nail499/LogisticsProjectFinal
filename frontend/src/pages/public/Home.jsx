import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Phone, Mail, MapPin, Clock, ChevronRight, ArrowRight, CheckCircle2,
  Truck, Ship, Plane, Warehouse, ShieldCheck, BarChart3, Star, Search, Menu, X,
} from 'lucide-react';
import Reveal from '../../components/Reveal.jsx';
import AnimatedCounter from '../../components/AnimatedCounter.jsx';
import LanguageSwitcher from '../../components/LanguageSwitcher.jsx';

import bannerHero from '../../assets/logzee/banner_slider_4.jpg';
import aboutImg from '../../assets/logzee/about_img_2.jpg';
import yearsImg from '../../assets/logzee/years_img.png';
import courierMan from '../../assets/logzee/courier-man.png';
import mapBg from '../../assets/logzee/map-bg.png';
import img1 from '../../assets/logzee/img-1.jpg';
import img2 from '../../assets/logzee/img-2.jpg';
import img3 from '../../assets/logzee/img-3.jpg';
import img7 from '../../assets/logzee/img-7.jpg';
import img8 from '../../assets/logzee/img-8.jpg';
import img9 from '../../assets/logzee/img-9.jpg';
import img10 from '../../assets/logzee/img-10.jpg';
import img11 from '../../assets/logzee/img-11.jpg';
import blogImg1 from '../../assets/logzee/blog_img_1.jpg';
import blogImg2 from '../../assets/logzee/blog_img_2.jpg';
import blogImg3 from '../../assets/logzee/blog_img_3.jpg';
import postThumb1 from '../../assets/logzee/post_thumb_1.jpg';
import postThumb2 from '../../assets/logzee/post_thumb_2.jpg';
import team1 from '../../assets/logzee/team_1.jpg';
import team2 from '../../assets/logzee/team_2.jpg';
import team3 from '../../assets/logzee/team_3.jpg';
import teamFeatured from '../../assets/logzee/team-1.jpg';
import logoFooter from '../../assets/fleetra-mark.svg';

// ---- small inline brand-social icons (lucide dropped brand marks) ----
const SocialIcon = ({ path, ...p }) => (
  <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor" {...p}>
    <path d={path} />
  </svg>
);
const SOCIALS = [
  { name: 'Facebook', path: 'M22 12a10 10 0 1 0-11.6 9.9v-7H7.9V12h2.5V9.8c0-2.5 1.5-3.9 3.8-3.9 1.1 0 2.2.2 2.2.2v2.5h-1.3c-1.2 0-1.6.8-1.6 1.6V12h2.8l-.4 2.9h-2.4v7A10 10 0 0 0 22 12Z' },
  { name: 'Instagram', path: 'M12 2c2.7 0 3.1 0 4.1.1 1 .1 1.7.2 2.3.5.6.2 1.1.6 1.6 1.1.5.5.8.9 1.1 1.6.2.6.4 1.3.5 2.3.1 1 .1 1.4.1 4.1s0 3.1-.1 4.1c-.1 1-.2 1.7-.5 2.3-.2.6-.6 1.1-1.1 1.6-.5.5-.9.8-1.6 1.1-.6.2-1.3.4-2.3.5-1 .1-1.4.1-4.1.1s-3.1 0-4.1-.1c-1-.1-1.7-.2-2.3-.5-.6-.2-1.1-.6-1.6-1.1-.5-.5-.8-.9-1.1-1.6-.2-.6-.4-1.3-.5-2.3C2 15.1 2 14.7 2 12s0-3.1.1-4.1c.1-1 .2-1.7.5-2.3.2-.6.6-1.1 1.1-1.6.5-.5.9-.8 1.6-1.1.6-.2 1.3-.4 2.3-.5C8.9 2 9.3 2 12 2Zm0 5a5 5 0 1 0 0 10 5 5 0 0 0 0-10Zm0 8.2a3.2 3.2 0 1 1 0-6.4 3.2 3.2 0 0 1 0 6.4Zm5.2-8.4a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4Z' },
  { name: 'X', path: 'M18.9 2H22l-7.2 8.3L23 22h-6.6l-5.2-6.7L5.2 22H2l7.7-8.8L2 2h6.7l4.7 6.2Zm-1.2 18h1.7L7.4 3.9H5.6Z' },
  { name: 'LinkedIn', path: 'M6.9 8.4H3.3V21h3.6ZM5.1 3a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2ZM21 21v-7c0-3.4-1.8-5-4.3-5-2 0-2.8 1.1-3.3 1.9V8.4H9.8c0 .1 0 12.6 0 12.6h3.6v-7c0-.4 0-.7.1-1 .3-.7.9-1.4 2-1.4 1.4 0 2 1.1 2 2.6v6.8Z' },
];

// Icons/images only — translated title/desc/role/quote/excerpt text is
// pulled from i18n at render time (see landing.service*/teamMember*/
// testimonial*/blog* keys in src/i18n/locales/*.json) and zipped in by index.
const SERVICE_ICONS = [Truck, Warehouse, Ship, Plane, ShieldCheck, BarChart3];
const GALLERY = [img3, img7, img1, img2, img8, img9, img10, img11];
const TEAM_META = [
  { photo: team1, name: 'Elvin Guliyev' },
  { photo: team2, name: 'Aygun Safarova' },
  { photo: team3, name: 'Gulay Huseynova' },
];
const TESTIMONIAL_NAMES = ['Rashad M.', 'Nargiz A.', 'Javid H.'];
const BLOG_META = [
  { img: blogImg1, date: 'Jul 12' },
  { img: blogImg2, date: 'Jun 28' },
  { img: blogImg3, date: 'Jun 05' },
];

export default function Home() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [scrolled, setScrolled] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [trackingInput, setTrackingInput] = useState('');

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const scrollToId = (id) => {
    setMobileNavOpen(false);
    if (id === 'top') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const handleTrackSubmit = (e) => {
    e.preventDefault();
    navigate('/tracking');
  };

  const NAV_LINKS = [
    { id: 'top', label: t('landing.navHome') },
    { id: 'about', label: t('landing.navAbout') },
    { id: 'services', label: t('landing.navServices') },
    { id: 'gallery', label: t('landing.navGallery') },
    { id: 'team', label: t('landing.navTeam') },
    { id: 'contact', label: t('landing.navContact') },
  ];

  const SERVICES = SERVICE_ICONS.map((icon, i) => ({
    icon,
    title: t(`landing.service${i + 1}Title`),
    desc: t(`landing.service${i + 1}Desc`),
  }));

  const TEAM = TEAM_META.map((m, i) => ({ ...m, role: t(`landing.teamMember${i + 1}Role`) }));

  const TESTIMONIALS = TESTIMONIAL_NAMES.map((name, i) => ({
    name,
    role: t(`landing.testimonial${i + 1}Role`),
    quote: t(`landing.testimonial${i + 1}Quote`),
  }));

  const BLOG = BLOG_META.map((m, i) => ({
    ...m,
    title: t(`landing.blog${i + 1}Title`),
    excerpt: t(`landing.blog${i + 1}Excerpt`),
  }));

  const ABOUT_BULLETS = [t('landing.aboutBullet1'), t('landing.aboutBullet2'), t('landing.aboutBullet3')];

  return (
    <div className="overflow-x-hidden bg-white text-[#4a5568]" style={{ fontFamily: "'Poppins', sans-serif" }}>
      {/* ============ TOPBAR ============ */}
      <div className="hidden border-b border-gray-100 bg-white px-6 py-2.5 text-[13px] text-gray-600 sm:block">
        <div className="mx-auto flex max-w-7xl items-center justify-between">
          <div className="flex items-center gap-6">
            <span className="flex items-center gap-1.5"><Phone size={13} className="text-[#fe8704]" /> +994 12 555 00 00</span>
            <span className="flex items-center gap-1.5"><Mail size={13} className="text-[#fe8704]" /> info@fleetra.io</span>
            <span className="flex items-center gap-1.5"><MapPin size={13} className="text-[#fe8704]" /> {t('landing.topbarLocation')}</span>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              {SOCIALS.map((s) => (
                <a key={s.name} href="#" aria-label={s.name} onClick={(e) => e.preventDefault()} className="transition-colors hover:text-[#fe8704]" style={{ color: '#9ca3af' }}>
                  <SocialIcon path={s.path} />
                </a>
              ))}
            </div>
            <LanguageSwitcher variant="default" />
          </div>
        </div>
      </div>

      {/* ============ HEADER / NAV ============ */}
      <header className={`sticky top-0 z-50 bg-white transition-shadow duration-300 ${scrolled ? 'shadow-md' : 'shadow-sm'}`}>
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <button type="button" onClick={() => scrollToId('top')} className="flex items-center gap-2.5">
              <img src={logoFooter} alt="Fleetra" className="h-8 w-auto" />
              <span className="text-xl font-extrabold text-[#111827]">Fleet<span className="text-[#fe8704]">ra</span></span>
          </button>

          <nav className="hidden items-center gap-10 lg:flex">
            {NAV_LINKS.map((l) => (
              <button
                key={l.id}
                type="button"
                onClick={() => scrollToId(l.id)}
                className="text-[15px] font-semibold text-[#374151] transition-colors hover:text-[#fe8704]"
              >
                {l.label}
              </button>
            ))}
          </nav>

          <div className="hidden items-center gap-4 lg:flex">
            <Link to="/login" className="text-[15px] font-semibold transition-colors hover:text-[#fe8704]" style={{ color: '#111827' }}>{t('landing.signIn')}</Link>
            <Link
              to="/register"
              className="flex items-center gap-1.5 rounded-md bg-[#fe8704] px-5 py-2.5 text-sm font-bold shadow-[0_8px_20px_-6px_rgba(254,135,4,0.55)] transition-transform hover:-translate-y-0.5"
              style={{ color: '#ffffff' }}
            >
              {t('nav.register')} <ArrowRight size={15} />
            </Link>
          </div>

          <button type="button" className="text-[#111827] lg:hidden" onClick={() => setMobileNavOpen((v) => !v)} aria-label="Menu">
            {mobileNavOpen ? <X size={26} /> : <Menu size={26} />}
          </button>
        </div>

        {mobileNavOpen && (
          <div className="border-t border-gray-100 bg-white px-6 py-4 lg:hidden">
            <div className="flex flex-col gap-3">
              {NAV_LINKS.map((l) => (
                <button key={l.id} type="button" onClick={() => scrollToId(l.id)} className="py-1.5 text-left text-[15px] font-semibold" style={{ color: '#111827' }}>
                  {l.label}
                </button>
              ))}
              <div className="mt-1 flex justify-start">
                <LanguageSwitcher variant="default" />
              </div>
              <div className="mt-2 flex gap-3 border-t border-gray-100 pt-4">
                <Link to="/login" className="flex-1 rounded-md border border-gray-200 py-2.5 text-center text-sm font-semibold" style={{ color: '#111827' }}>{t('landing.signIn')}</Link>
                <Link to="/register" className="flex-1 rounded-md bg-[#fe8704] py-2.5 text-center text-sm font-bold" style={{ color: '#ffffff' }}>{t('nav.register')}</Link>
              </div>
            </div>
          </div>
        )}
      </header>

      {/* ============ HERO ============ */}
      <section id="top" className="relative overflow-hidden bg-black pb-28 pt-20 sm:pt-28">
        <div
          className="absolute inset-0 bg-cover bg-center"
          style={{ backgroundImage: `url(${bannerHero})` }}
        />
        <div className="absolute inset-0 bg-black/72" />

        <div className="relative mx-auto max-w-4xl px-6 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-[#fe8704]/50 bg-[#fe8704]/10 px-4 py-1.5 text-[13px] font-bold uppercase tracking-wider text-[#fe8704]">
            {t('landing.heroBadge')}
          </span>
          <h1
            className="mt-7 text-center font-extrabold leading-tight"
            style={{
              color: '#ffffff',
              textShadow: '0 2px 18px rgba(0,0,0,0.55)',
              fontSize: 'clamp(2.75rem, 6vw, 5rem)',
              lineHeight: 1.1,
            }}
          >
            {t('landing.heroTitleLine1')}<br />
            <span className="text-[#fe8704]">{t('landing.heroTitleLine2')}</span>
          </h1>
          <p
            className="max-w-2xl text-base sm:text-lg"
            style={{
              color: '#f1f5f9',
              textShadow: '0 1px 10px rgba(0,0,0,0.5)',
              textAlign: 'center',
              marginLeft: 'auto',
              marginRight: 'auto',
              marginTop: '2rem',
              marginBottom: 0,
            }}
          >
            {t('landing.heroLead')}
          </p>
          <div className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Link
              to="/register"
              className="flex w-full items-center justify-center gap-2 rounded-md bg-[#fe8704] px-8 py-3.5 text-sm font-bold shadow-[0_10px_25px_-6px_rgba(254,135,4,0.6)] transition-transform hover:-translate-y-0.5 sm:w-auto"
              style={{ color: '#ffffff' }}
            >
              {t('landing.heroCtaQuote')} <ArrowRight size={16} />
            </Link>
            <button
              type="button"
              onClick={() => navigate('/apply')}
              className="w-full rounded-md border border-white/40 bg-white/10 px-8 py-3.5 text-sm font-bold text-white transition-colors hover:bg-white/20 sm:w-auto"
            >
              {t('landing.heroCtaJoinDriver')}
            </button>
          </div>
        </div>

        {/* quote / quick-access overlap card */}
        <div className="relative mx-auto mt-16 max-w-5xl px-6">
          <div className="grid grid-cols-1 divide-y divide-gray-100 rounded-xl bg-white shadow-2xl sm:grid-cols-3 sm:divide-x sm:divide-y-0">
            <form onSubmit={handleTrackSubmit} className="flex items-center gap-3 p-6">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#fe8704]/10 text-[#fe8704]"><Search size={19} /></span>
              <div className="min-w-0 flex-1">
                <div className="text-xs font-bold uppercase tracking-wide text-gray-400">{t('landing.quickTrackLabel')}</div>
                <input
                  value={trackingInput}
                  onChange={(e) => setTrackingInput(e.target.value)}
                  placeholder={t('landing.quickTrackPlaceholder')}
                  className="mt-1 w-full border-none p-0 text-[15px] font-semibold text-[#111827] outline-none placeholder:font-normal placeholder:text-gray-400"
                />
              </div>
              <button type="submit" aria-label="Track" className="shrink-0 text-[#111827]"><ArrowRight size={18} /></button>
            </form>

            <button type="button" onClick={() => navigate('/register')} className="flex items-center gap-3 p-6 text-left transition-colors hover:bg-gray-50">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#fe8704]/10 text-[#fe8704]"><Truck size={19} /></span>
              <div>
                <div className="text-xs font-bold uppercase tracking-wide text-gray-400">{t('landing.quickCargoLabel')}</div>
                <div className="mt-1 text-[15px] font-semibold text-[#111827]">{t('landing.quickCargoValue')}</div>
              </div>
            </button>

            <button type="button" onClick={() => navigate('/apply')} className="flex items-center gap-3 p-6 text-left transition-colors hover:bg-gray-50">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#fe8704]/10 text-[#fe8704]"><ShieldCheck size={19} /></span>
              <div>
                <div className="text-xs font-bold uppercase tracking-wide text-gray-400">{t('landing.quickDriverLabel')}</div>
                <div className="mt-1 text-[15px] font-semibold text-[#111827]">{t('landing.quickDriverValue')}</div>
              </div>
            </button>
          </div>
        </div>
      </section>

      {/* ============ ABOUT ============ */}
      <section id="about" className="bg-white px-6 py-24">
        <div className="mx-auto grid max-w-6xl items-center gap-16 lg:grid-cols-2">
          <Reveal>
            <div className="relative mx-auto max-w-sm">
              <div className="absolute -left-5 -top-5 h-full w-full rounded-2xl bg-[#fe8704]/15" />
              <img src={aboutImg} alt="About Fleetra" className="relative w-full rounded-2xl object-cover shadow-xl" />
              <div className="absolute -bottom-8 -right-6 rounded-xl bg-white p-3 shadow-2xl sm:-right-10">
                <img src={yearsImg} alt="Experience" className="h-16 w-auto sm:h-20" />
              </div>
            </div>
          </Reveal>

          <Reveal delay={120}>
            <div>
              <span className="text-[13px] font-bold uppercase tracking-widest text-[#fe8704]">{t('landing.aboutKicker')}</span>
              <h2 className="mt-3 font-extrabold text-[#111827]" style={{ fontSize: 'clamp(1.75rem, 4vw, 2.5rem)' }}>
                {t('landing.aboutTitle')}
              </h2>
              <p className="mt-5 text-[15px] leading-relaxed text-gray-500">
                {t('landing.aboutLead')}
              </p>
              <ul className="mt-6 space-y-3">
                {ABOUT_BULLETS.map((item) => (
                  <li key={item} className="flex items-start gap-2.5 text-[15px] text-gray-600">
                    <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-[#fe8704]" />
                    {item}
                  </li>
                ))}
              </ul>
              <div className="mt-9 flex flex-wrap items-center gap-6">
                <button
                  type="button"
                  onClick={() => scrollToId('services')}
                  className="flex items-center gap-2 rounded-md bg-[#111827] px-7 py-3.5 text-sm font-bold text-white transition-transform hover:-translate-y-0.5"
                >
                  {t('landing.aboutCtaServices')} <ChevronRight size={16} />
                </button>
                <div className="flex items-center gap-3">
                  <span className="flex h-12 w-12 items-center justify-center rounded-full bg-[#fe8704]/10 text-[#fe8704]"><Phone size={19} /></span>
                  <div>
                    <div className="text-[13px] text-gray-400">{t('landing.aboutSupportLabel')}</div>
                    <div className="text-[15px] font-bold text-[#111827]">+994 12 555 00 00</div>
                  </div>
                </div>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      {/* ============ SERVICES ============ */}
      <section id="services" className="bg-[#f7f8fb] px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <Reveal>
            <div className="mx-auto mb-14 max-w-xl text-center">
              <span className="text-[13px] font-bold uppercase tracking-widest text-[#fe8704]">{t('landing.servicesKicker')}</span>
              <h2 className="mt-3 font-extrabold text-[#111827]" style={{ fontSize: 'clamp(1.75rem, 4vw, 2.5rem)' }}>{t('landing.servicesTitle')}</h2>
              <p className="mt-4 text-[15px] text-gray-500">{t('landing.servicesLead')}</p>
            </div>
          </Reveal>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {SERVICES.map((s, i) => (
              <Reveal key={s.title} delay={i * 80}>
                <div className="group h-full rounded-2xl border border-gray-100 bg-white p-8 shadow-sm transition-all hover:-translate-y-1.5 hover:shadow-xl">
                  <span className="flex h-14 w-14 items-center justify-center rounded-xl bg-[#111827] text-[#fe8704] transition-colors group-hover:bg-[#fe8704] group-hover:text-white">
                    <s.icon size={26} />
                  </span>
                  <h3 className="mt-6 font-bold text-[#111827]" style={{ fontSize: '1.125rem' }}>{s.title}</h3>
                  <p className="mt-2.5 text-[15px] leading-relaxed text-gray-500">{s.desc}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ============ COUNTER / FUN FACTS ============ */}
      <section className="bg-white px-6 py-20">
        <div className="mx-auto grid max-w-6xl grid-cols-2 gap-8 rounded-2xl border border-gray-100 bg-[#f7f8fb] px-6 py-12 text-center sm:grid-cols-4">
          {[
            { to: 25, suffix: '+', label: t('landing.counterYears') },
            { to: 1200, suffix: '+', label: t('landing.counterTrips') },
            { to: 150, suffix: '+', label: t('landing.counterDrivers') },
            { to: 10, suffix: '', label: t('landing.counterCities') },
          ].map((s) => (
            <Reveal key={s.label}>
              <div>
                <div className="text-4xl font-extrabold text-[#fe8704] sm:text-5xl">
                  <AnimatedCounter to={s.to} suffix={s.suffix} />
                </div>
                <div className="mt-2 text-[13px] font-semibold uppercase tracking-wider text-gray-500 sm:text-sm">{s.label}</div>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ============ GALLERY ============ */}
      <section id="gallery" className="bg-white px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <Reveal>
            <div className="mx-auto mb-14 max-w-xl text-center">
              <span className="text-[13px] font-bold uppercase tracking-widest text-[#fe8704]">{t('landing.galleryKicker')}</span>
              <h2 className="mt-3 font-extrabold text-[#111827]" style={{ fontSize: 'clamp(1.75rem, 4vw, 2.5rem)' }}>{t('landing.galleryTitle')}</h2>
            </div>
          </Reveal>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {GALLERY.map((src, i) => (
              <Reveal key={i} delay={i * 50}>
                <div className="group relative aspect-square overflow-hidden rounded-xl">
                  <img src={src} alt="" className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110" />
                  <div className="absolute inset-0 flex items-center justify-center bg-black/0 opacity-0 transition-all group-hover:bg-black/50 group-hover:opacity-100">
                    <span className="flex h-11 w-11 items-center justify-center rounded-full bg-[#fe8704] text-white"><ArrowRight size={18} /></span>
                  </div>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ============ CTA / PROMO BANNER ============ */}
      <section className="bg-[#fff5ea] px-6 py-20">
        <div className="mx-auto grid max-w-6xl items-center gap-10 lg:grid-cols-2">
          <Reveal>
            <div>
              <span className="text-[13px] font-bold uppercase tracking-widest text-[#fe8704]">{t('landing.ctaKicker')}</span>
              <h2 className="mt-3 font-extrabold text-[#111827]" style={{ fontSize: 'clamp(1.75rem, 4vw, 2.5rem)' }}>
                {t('landing.ctaTitle')}
              </h2>
              <p className="mt-4 max-w-md text-[15px] leading-relaxed text-gray-600">
                {t('landing.ctaLead')}
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <Link to="/register" className="flex items-center gap-2 rounded-md bg-[#fe8704] px-7 py-3.5 text-sm font-bold transition-transform hover:-translate-y-0.5" style={{ color: '#ffffff' }}>
                  {t('landing.ctaCustomerSignup')} <ArrowRight size={16} />
                </Link>
                <button type="button" onClick={() => navigate('/apply')} className="rounded-md border border-gray-300 bg-white px-7 py-3.5 text-sm font-bold text-[#111827] transition-colors hover:bg-gray-50">
                  {t('landing.ctaDriverApplication')}
                </button>
              </div>
            </div>
          </Reveal>
          <Reveal delay={150}>
            <div className="hidden justify-end lg:flex">
              <img src={courierMan} alt="Courier" className="h-auto max-h-[340px] w-auto object-contain" />
            </div>
          </Reveal>
        </div>
      </section>

      {/* ============ TEAM ============ */}
      <section id="team" className="bg-white px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <Reveal>
            <div className="mx-auto mb-14 max-w-xl text-center">
              <span className="text-[13px] font-bold uppercase tracking-widest text-[#fe8704]">{t('landing.teamKicker')}</span>
              <h2 className="mt-3 font-extrabold text-[#111827]" style={{ fontSize: 'clamp(1.75rem, 4vw, 2.5rem)' }}>{t('landing.teamTitle')}</h2>
            </div>
          </Reveal>
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
            <Reveal className="lg:col-span-1">
              <div className="group relative overflow-hidden rounded-2xl shadow-lg">
                <img src={teamFeatured} alt="Chief Executive Officer" className="h-full w-full object-cover" />
                <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/85 to-transparent p-6 pt-16">
                  <h3 className="font-bold text-white" style={{ fontSize: '1.125rem' }}>Tural Mammadov</h3>
                  <p className="text-[13px] font-semibold text-[#fe8704]">{t('landing.ceoRole')}</p>
                </div>
              </div>
            </Reveal>
            <div className="grid grid-cols-1 gap-8 sm:grid-cols-3 lg:col-span-2">
              {TEAM.map((m, i) => (
                <Reveal key={m.name} delay={i * 90}>
                  <div className="group relative overflow-hidden rounded-2xl shadow-md">
                    <img src={m.photo} alt={m.name} className="aspect-square w-full object-cover transition-transform duration-500 group-hover:scale-105" />
                    <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/90 to-transparent p-4">
                      <h4 className="font-bold text-white" style={{ fontSize: '0.875rem' }}>{m.name}</h4>
                      <p className="text-[11px] font-medium text-[#fe8704]">{m.role}</p>
                    </div>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ============ TESTIMONIALS ============ */}
      <section className="bg-[#f7f8fb] px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <Reveal>
            <div className="mx-auto mb-14 max-w-xl text-center">
              <span className="text-[13px] font-bold uppercase tracking-widest text-[#fe8704]">{t('landing.testimonialsKicker')}</span>
              <h2 className="mt-3 font-extrabold text-[#111827]" style={{ fontSize: 'clamp(1.75rem, 4vw, 2.5rem)' }}>{t('landing.testimonialsTitle')}</h2>
            </div>
          </Reveal>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
            {TESTIMONIALS.map((tm, i) => (
              <Reveal key={tm.name} delay={i * 100}>
                <div className="h-full rounded-2xl bg-white p-7 shadow-md">
                  <div className="flex gap-1 text-[#fe8704]">
                    {Array.from({ length: 5 }).map((_, si) => <Star key={si} size={14} fill="currentColor" strokeWidth={0} />)}
                  </div>
                  <p className="mt-4 min-h-[90px] text-[15px] italic leading-relaxed text-gray-600">&ldquo;{tm.quote}&rdquo;</p>
                  <div className="mt-5 border-t border-gray-100 pt-4">
                    <div className="text-[15px] font-bold text-[#111827]">{tm.name}</div>
                    <div className="text-[13px] text-gray-400">{tm.role}</div>
                  </div>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ============ BLOG ============ */}
      <section className="bg-white px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <Reveal>
            <div className="mx-auto mb-14 max-w-xl text-center">
              <span className="text-[13px] font-bold uppercase tracking-widest text-[#fe8704]">{t('landing.blogKicker')}</span>
              <h2 className="mt-3 font-extrabold text-[#111827]" style={{ fontSize: 'clamp(1.75rem, 4vw, 2.5rem)' }}>{t('landing.blogTitle')}</h2>
            </div>
          </Reveal>
          <div className="grid grid-cols-1 gap-8 sm:grid-cols-3">
            {BLOG.map((b, i) => (
              <Reveal key={b.title} delay={i * 90}>
                <article className="group overflow-hidden rounded-2xl border border-gray-100 shadow-sm transition-shadow hover:shadow-xl">
                  <div className="overflow-hidden">
                    <img src={b.img} alt={b.title} className="aspect-[5/4] w-full object-cover transition-transform duration-500 group-hover:scale-110" />
                  </div>
                  <div className="p-6">
                    <span className="flex items-center gap-1.5 text-[13px] font-semibold text-[#fe8704]"><Clock size={12} /> {b.date}</span>
                    <h3 className="mt-2.5 font-bold leading-snug text-[#111827]" style={{ fontSize: '1rem' }}>{b.title}</h3>
                    <p className="mt-2 text-[15px] leading-relaxed text-gray-500">{b.excerpt}</p>
                  </div>
                </article>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ============ FOOTER ============ */}
      <footer id="contact" className="border-t border-gray-100 bg-[#f7f8fb] px-6 pt-20 text-gray-600">
        <div className="mx-auto grid max-w-6xl grid-cols-1 gap-20 pb-14 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <div className="flex items-center gap-2.5">
              <img src={logoFooter} alt="Fleetra" className="h-9 w-auto" />
              <span className="text-xl font-extrabold text-[#111827]">Fleet<span className="text-[#fe8704]">ra</span></span>
            </div>
            <p className="mt-4 max-w-[260px] text-[13.5px] leading-relaxed text-gray-500">
              {t('landing.footerDesc')}
            </p>
            <div className="mt-5 flex gap-3">
              {SOCIALS.map((s) => (
                <a key={s.name} href="#" aria-label={s.name} onClick={(e) => e.preventDefault()} className="flex h-9 w-9 items-center justify-center rounded-full bg-white shadow-sm transition-colors hover:bg-[#fe8704] hover:text-white" style={{ color: '#6b7280' }}>
                  <SocialIcon path={s.path} />
                </a>
              ))}
            </div>
          </div>

          <div>
            <h4 className="font-bold uppercase tracking-wide text-[#111827]" style={{ fontSize: '0.875rem' }}>{t('landing.footerQuickLinks')}</h4>
            <ul className="mt-5 space-y-2.5 text-[15px]">
              <li><button type="button" onClick={() => scrollToId('about')} className="transition-colors hover:text-[#fe8704]" style={{ color: '#6b7280' }}>{t('landing.navAbout')}</button></li>
              <li><button type="button" onClick={() => scrollToId('services')} className="transition-colors hover:text-[#fe8704]" style={{ color: '#6b7280' }}>{t('landing.navServices')}</button></li>
              <li><button type="button" onClick={() => scrollToId('gallery')} className="transition-colors hover:text-[#fe8704]" style={{ color: '#6b7280' }}>{t('landing.navGallery')}</button></li>
              <li><Link to="/tracking" className="transition-colors hover:text-[#fe8704]" style={{ color: '#6b7280' }}>{t('landing.quickTrackLabel')}</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold uppercase tracking-wide text-[#111827]" style={{ fontSize: '0.875rem' }}>{t('landing.footerHours')}</h4>
            <ul className="mt-5 space-y-2.5 text-[15px]" style={{ color: '#6b7280' }}>
              <li className="flex items-center justify-between gap-4"><span>{t('landing.footerMonFri')}</span><span className="font-medium text-[#111827]">08:00 – 18:00</span></li>
              <li className="flex items-center justify-between gap-4"><span>{t('landing.footerSat')}</span><span className="font-medium text-[#111827]">09:00 – 15:00</span></li>
              <li className="flex items-center justify-between gap-4"><span>{t('landing.footerSun')}</span><span className="font-medium text-[#111827]">{t('landing.footerClosed')}</span></li>
              <li className="flex items-center justify-between gap-4"><span>{t('landing.footerDispatchTracking')}</span><span className="font-medium text-[#fe8704]">24/7</span></li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold uppercase tracking-wide text-[#111827]" style={{ fontSize: '0.875rem' }}>{t('landing.footerFindUs')}</h4>
            <div className="relative mt-5 overflow-hidden rounded-lg border border-gray-200">
              <img src={mapBg} alt="Map" className="w-full" />
              <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-[#fe8704] p-1.5 shadow-lg">
                <MapPin size={14} className="text-white" />
              </span>
            </div>
            <div className="mt-4 flex gap-3">
              <img src={postThumb1} alt="" className="h-12 w-12 rounded-md object-cover" />
              <img src={postThumb2} alt="" className="h-12 w-12 rounded-md object-cover" />
              <div className="flex-1 self-center text-[13px] text-gray-500">{t('landing.footerGalleryCaption')}</div>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-200 py-6">
          <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 text-[13px] text-gray-500 sm:flex-row">
            <span>{t('landing.footerCopyright')}</span>
            <span>{t('landing.footerSystemTagline')}</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
