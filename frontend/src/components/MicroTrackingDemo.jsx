// Interactive micro-tracking demo for the landing page.
// Entering the test tracking ID "12345" animates a truck marker sliding
// along a mock route on a small embedded Leaflet map — a stand-in for a
// real OSRM-routed, WebSocket-driven live position feed (Stage 6 wiring
// point: swap `ROUTE` for a fetched OSRM polyline and the rAF loop for a
// WebSocket position stream).
import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MapContainer, TileLayer, Marker, Polyline } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Search, MapPin, Clock3, Radio } from 'lucide-react';

const ROUTE = [
  [40.3717, 49.8920], // origin warehouse (Baku port / industrial zone)
  [40.3805, 49.8801],
  [40.3902, 49.8715],
  [40.3968, 49.8688],
  [40.4093, 49.8671], // destination (central Baku)
];
const DURATION_MS = 9000;

function pointOnRoute(progress) {
  const segCount = ROUTE.length - 1;
  const scaled = Math.min(progress, 1) * segCount;
  const segIndex = Math.min(Math.floor(scaled), segCount - 1);
  const segT = scaled - segIndex;
  const [lat1, lng1] = ROUTE[segIndex];
  const [lat2, lng2] = ROUTE[segIndex + 1];
  return [lat1 + (lat2 - lat1) * segT, lng1 + (lng2 - lng1) * segT];
}

const truckIcon = new L.DivIcon({
  className: 'demo-truck-icon',
  html: '<div style="font-size:22px;filter:drop-shadow(0 2px 4px rgba(0,0,0,0.5))">🚛</div>',
  iconSize: [28, 28],
  iconAnchor: [14, 14],
});
const pinIconStart = new L.DivIcon({
  className: 'demo-pin',
  html: '<div style="width:14px;height:14px;border-radius:50%;background:#3B82F6;border:2px solid white;box-shadow:0 0 0 4px rgba(59,130,246,0.25)"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});
const pinIconEnd = new L.DivIcon({
  className: 'demo-pin',
  html: '<div style="width:14px;height:14px;border-radius:50%;background:#22FFB0;border:2px solid white;box-shadow:0 0 0 4px rgba(34,255,176,0.25)"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

export default function MicroTrackingDemo() {
  const { t } = useTranslation();
  const [input, setInput] = useState('');
  const [simulating, setSimulating] = useState(false);
  const [notFound, setNotFound] = useState(false);
  const [progress, setProgress] = useState(0);
  const [delivered, setDelivered] = useState(false);
  const rafRef = useRef(null);
  const startRef = useRef(null);

  const truckPos = useMemo(() => pointOnRoute(progress), [progress]);
  const etaSeconds = Math.max(0, Math.round((1 - progress) * 480)); // cosmetic 8-min countdown
  const etaLabel = `${Math.floor(etaSeconds / 60)}:${String(etaSeconds % 60).padStart(2, '0')}`;

  useEffect(() => {
    if (!simulating) return undefined;
    const tick = (now) => {
      if (!startRef.current) startRef.current = now;
      const elapsed = now - startRef.current;
      const p = Math.min(elapsed / DURATION_MS, 1);
      setProgress(p);
      if (p < 1) {
        rafRef.current = requestAnimationFrame(tick);
      } else {
        setDelivered(true);
      }
    };
    rafRef.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafRef.current);
  }, [simulating]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (input.trim() === '12345') {
      setNotFound(false);
      setDelivered(false);
      setProgress(0);
      startRef.current = null;
      setSimulating(true);
    } else {
      setSimulating(false);
      setNotFound(true);
    }
  };

  return (
    <div className="mx-auto max-w-3xl rounded-2xl border border-base-700 bg-base-900/70 p-6 shadow-glow sm:p-8">
      <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={17} />
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={t('landing.trackingPlaceholder')}
            className="w-full rounded-xl border border-base-600 bg-base-800 py-3 pl-10 pr-3 text-sm text-white placeholder-slate-500 outline-none focus:border-accent-blue"
          />
        </div>
        <button
          type="submit"
          className="rounded-xl bg-accent-blue px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-accent-blueDark"
        >
          {t('landing.trackingButton')}
        </button>
      </form>
      <p className="mt-2.5 text-xs text-slate-500">{t('landing.trackingHint')}</p>

      {notFound && (
        <div className="mt-5 rounded-xl border border-anomaly-red/30 bg-anomaly-red/10 px-4 py-3 text-sm text-anomaly-red">
          {t('landing.trackingNotFound')}
        </div>
      )}

      {simulating && (
        <div className="mt-6 overflow-hidden rounded-xl border border-base-700">
          <div className="flex flex-wrap items-center justify-between gap-3 bg-base-800 px-4 py-3">
            <div className="flex items-center gap-2">
              <span className="relative flex h-2.5 w-2.5">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-neon-green opacity-60" />
                <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-neon-green" />
              </span>
              <span className="text-xs font-bold tracking-wide text-neon-green">
                {delivered ? t('status.DELIVERED') : t('status.IN_TRANSIT')}
              </span>
              <span className="rounded-full bg-white/5 px-2 py-0.5 text-[10px] font-semibold text-slate-400">
                {t('landing.trackingSimBadge')}
              </span>
            </div>
            {!delivered && (
              <div className="flex items-center gap-1.5 text-xs text-slate-300">
                <Clock3 size={13} /> ETA {etaLabel}
              </div>
            )}
          </div>
          <div className="h-64 w-full">
            <MapContainer center={[40.39, 49.878]} zoom={13} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
              <TileLayer
                attribution="OpenStreetMap"
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <Polyline positions={ROUTE} pathOptions={{ color: '#3B82F6', weight: 4, opacity: 0.8 }} />
              <Marker position={ROUTE[0]} icon={pinIconStart} />
              <Marker position={ROUTE[ROUTE.length - 1]} icon={pinIconEnd} />
              <Marker position={truckPos} icon={truckIcon} />
            </MapContainer>
          </div>
          <div className="flex items-center justify-between gap-3 bg-base-800 px-4 py-3 text-xs text-slate-400">
            <span className="flex items-center gap-1.5"><MapPin size={12} className="text-accent-blue" /> Anbar — Bakı</span>
            <span className="flex items-center gap-1.5"><Radio size={12} className="text-neon-green" /> #12345</span>
            <span className="flex items-center gap-1.5"><MapPin size={12} className="text-neon-green" /> Mərkəz — Bakı</span>
          </div>
        </div>
      )}

      <p className="mt-5 text-center text-xs text-slate-500">
        {t('landing.trackingRealLink')}{' '}
        <a href="/tracking" className="font-semibold text-accent-blue hover:underline">
          {t('landing.trackingRealLinkCta')}
        </a>
      </p>
    </div>
  );
}
