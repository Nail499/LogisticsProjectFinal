// Stage 3 — premium 30/70 split live-tracking dashboard.
// Left (30%): status, ETA countdown, pickup->destination timeline, driver card.
// Right (70%): full-height Leaflet map with route + live truck marker.
// Shared by the public /tracking search results AND the authenticated
// customer /customer/track/:trackingNumber view — same component, same data
// shape (backend: PublicTrackingResponse). Stage 6 wires the real WebSocket
// GPS feed in via subscribeTopic below: data.lastLatitude/lastLongitude now
// update live, re-rendering the truck marker automatically.
//
// Rethemed to match the site's light Fleetra theme (was the old dark
// "Control Tower" palette — bg-base-900/border-base-700/text-slate-* etc.)
// — same conversion pattern used elsewhere this session (ControlTowerMap,
// BackhaulMatcher, CapacityCheckModal...): var(--primary)/var(--success)/
// var(--danger)/var(--text-muted), #e5e7eb borders, #f9fafb/#fff surfaces.
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MapContainer, TileLayer, Marker, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Phone, IdCard, User, Clock3, MapPin, PackageSearch, Radio, Receipt, AlertTriangle, Globe2, FileText, ShieldCheck, Flag, Images, CheckCircle2 } from 'lucide-react';
import { subscribeTopic } from '../utils/socket.js';
import RoadRoutePolyline from './RoadRoutePolyline.jsx';
import PhotoLightbox from './PhotoLightbox.jsx';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const EXPENSE_CATEGORY_KEY = {
  FUEL: { key: 'categoryFuel', icon: '⛽' },
  TOLL: { key: 'categoryToll', icon: '🛣️' },
  FOOD: { key: 'categoryFood', icon: '🍽️' },
  MAINTENANCE: { key: 'categoryMaintenance', icon: '🔧' },
  OTHER: { key: 'categoryOther', icon: '📋' },
};

const DOCUMENT_KEY = {
  INVOICE: 'docInvoice',
  PACKING_LIST: 'docPackingList',
  CERTIFICATE_OF_ORIGIN: 'docCertOrigin',
  CMR: 'docCMR',
  BILL_OF_LADING: 'docBillOfLading',
  TRANSIT_DOCUMENT: 'docTransit',
  OTHER: 'docOther',
};

const TRANSPORT_MODE_KEY = { TRUCK: 'newCargoTransportTruck', RAIL: 'newCargoTransportRail', SEA: 'newCargoTransportSea', AIR: 'newCargoTransportAir' };

const DECLARATION_STATUS_KEY = { DRAFT: 'declarationDraft', SUBMITTED: 'declarationSubmitted', CLEARED: 'declarationCleared', REJECTED: 'declarationRejected' };
const DOCUMENT_STATUS_KEY = { PENDING: 'docStatusPending', VERIFIED: 'docStatusVerified', REJECTED: 'docStatusRejected' };
const BORDER_STATUS_KEY = { PENDING: 'borderStatusPending', CLEARED: 'borderStatusCleared', HELD: 'borderStatusHeld' };

// Fleetra brand orange (#fe8704) for the pickup pin + route line — was a
// generic blue left over from before the site settled on orange as the
// brand color (see Home.jsx / DashboardLayout's theme-orange). Destination
// stays green: a distinct "arrival" color, not a brand color, so no clash.
const pinStart = new L.DivIcon({
  className: '',
  html: '<div style="width:16px;height:16px;border-radius:50%;background:#fe8704;border:2.5px solid white;box-shadow:0 0 0 5px rgba(254,135,4,0.22)"></div>',
  iconSize: [16, 16], iconAnchor: [8, 8],
});
const pinEnd = new L.DivIcon({
  className: '',
  html: '<div style="width:16px;height:16px;border-radius:50%;background:#16a34a;border:2.5px solid white;box-shadow:0 0 0 5px rgba(22,163,74,0.22)"></div>',
  iconSize: [16, 16], iconAnchor: [8, 8],
});
const truckIcon = new L.DivIcon({
  className: '',
  html: '<div style="font-size:24px;filter:drop-shadow(0 2px 4px rgba(0,0,0,0.35))">🚛</div>',
  iconSize: [30, 30], iconAnchor: [15, 15],
});

// The live "where's my truck" marker used to be a plain emoji regardless of
// who's driving. Now that drivers can upload a profile photo (ProfilePage),
// show it here too — a small circular avatar with a tiny truck badge in the
// corner — so the customer sees an actual face on the map, not just a dot.
// Falls back to the old plain truck emoji when the driver has no photo yet.
function buildTruckIcon(driverPhotoUrl) {
  if (!driverPhotoUrl) return truckIcon;
  const photoSrc = `${API_BASE}${driverPhotoUrl}`;
  return new L.DivIcon({
    className: '',
    html: `
      <div style="position:relative;width:38px;height:38px;">
        <div style="width:36px;height:36px;border-radius:50%;overflow:hidden;background:#fff;border:2.5px solid #fe8704;box-shadow:0 2px 6px rgba(0,0,0,0.35);">
          <img src="${photoSrc}" style="width:100%;height:100%;object-fit:cover;display:block;" />
        </div>
        <div style="position:absolute;bottom:-3px;right:-3px;width:17px;height:17px;border-radius:50%;background:#fe8704;display:flex;align-items:center;justify-content:center;font-size:9px;line-height:1;border:2px solid #fff;box-shadow:0 1px 3px rgba(0,0,0,0.3);">🚛</div>
      </div>`,
    iconSize: [38, 38],
    iconAnchor: [19, 19],
  });
}

const STATUS_DOT = {
  PENDING: '#9ca3af',
  ASSIGNED: 'var(--primary)',
  IN_TRANSIT: 'var(--success)',
  DELIVERED: 'var(--success)',
};

function FitBounds({ points }) {
  const map = useMap();
  useEffect(() => {
    if (points.length > 1) {
      map.fitBounds(points, { padding: [40, 40] });
    } else if (points.length === 1) {
      map.setView(points[0], 13);
    }
  }, [map, JSON.stringify(points)]);
  return null;
}

// Bu panel çox vaxt modal/pəncərə içində (bax TripDetailModal) və ya
// tab keçidindən sonra ekrana gəlir — Leaflet konteynerin ölçüsünü YALNIZ
// ilk mount anında ölçür, ona görə əgər həmin an konteyner hələ son
// hündürlüyünü almayıbsa (CSS layout/transition tam oturmayıb), xəritə
// yarımçıq/səhv ölçüdə "donub qalır" (klassik Leaflet-in modal içində
// sındığı hal). invalidateSize() ölçünü məcburi yenidən hesablatır.
function InvalidateSizeOnMount() {
  const map = useMap();
  useEffect(() => {
    const id = setTimeout(() => map.invalidateSize(), 150);
    return () => clearTimeout(id);
  }, [map]);
  return null;
}

function formatClockTime(date, locale) {
  return date.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
}

// Turns a remaining-duration estimate into an actual arrival clock time —
// "Bu gün, 18:42" / "Sabah, 09:15" — since "neçəyə çatacaq" is more useful
// than a raw duration. The duration itself comes from OSRM's road-network
// routing (RoadRoutePolyline/fetchRoadRoute): realistic road-type/speed-
// limit based timing, which is a real improvement over the old straight-
// line 45km/h guess — but it is NOT live traffic telemetry (no paid traffic
// API is wired up here), so this is a "normal road conditions" ETA, not a
// "right now with today's jams" one.
function formatArrivalClock(durationMin, locale, t) {
  if (durationMin == null || !Number.isFinite(durationMin)) return null;
  const now = new Date();
  const arrival = new Date(now.getTime() + durationMin * 60000);
  const dayDiff = Math.round((new Date(arrival.toDateString()) - new Date(now.toDateString())) / 86400000);
  const time = formatClockTime(arrival, locale);
  if (dayDiff <= 0) return `${t('tracking.todayLabel')}, ${time}`;
  if (dayDiff === 1) return `${t('tracking.tomorrowLabel')}, ${time}`;
  return `${arrival.toLocaleDateString(locale, { day: 'numeric', month: 'short' })}, ${time}`;
}

// Formats a backend ISO LocalDateTime string into a short stage-timeline
// timestamp — e.g. "27 iyul, 14:32".
function formatStageTime(isoString, locale) {
  if (!isoString) return null;
  const d = new Date(isoString);
  if (Number.isNaN(d.getTime())) return null;
  return `${d.toLocaleDateString(locale, { day: 'numeric', month: 'short' })}, ${formatClockTime(d, locale)}`;
}

function useCountdown(etaMinutes, active) {
  const [remaining, setRemaining] = useState(etaMinutes != null ? etaMinutes * 60 : null);

  useEffect(() => {
    setRemaining(etaMinutes != null ? etaMinutes * 60 : null);
  }, [etaMinutes]);

  useEffect(() => {
    if (!active || remaining == null) return undefined;
    const id = setInterval(() => {
      setRemaining((r) => (r != null && r > 0 ? r - 1 : 0));
    }, 1000);
    return () => clearInterval(id);
  }, [active, remaining == null]);

  return remaining;
}

// small reusable "panel" card used throughout the left column
function SidePanel({ icon, title, children, style }) {
  return (
    <div
      style={{
        border: '1px solid var(--border)',
        background: 'var(--bg)',
        borderRadius: 12,
        padding: 16,
        ...style,
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          fontSize: 11,
          fontWeight: 600,
          textTransform: 'uppercase',
          letterSpacing: 0.4,
          color: 'var(--text-muted)',
          marginBottom: 8,
        }}
      >
        {icon} {title}
      </div>
      {children}
    </div>
  );
}

export default function LiveTrackingPanel({ data: initialData }) {
  const { t, i18n } = useTranslation();

  // Stage 6: start from the REST snapshot, then merge in live WebSocket
  // pushes (see backend TripBroadcastService) as the driver's position/
  // status changes — no more waiting on a poll interval.
  const [data, setData] = useState(initialData);
  const [isLive, setIsLive] = useState(false);
  useEffect(() => setData(initialData), [initialData]);

  // Real road-route stats from OSRM (see RoadRoutePolyline below): how much
  // of the road route is already behind the truck vs. still ahead, plus a
  // realistic remaining-time estimate based on actual road speeds (not the
  // old straight-line guess). Reset whenever the tracking number changes —
  // this component instance is reused across searches (see TrackingSearch),
  // so without this a new search would briefly show the previous shipment's
  // numbers until the new route loads.
  const [routeStats, setRouteStats] = useState(null);
  useEffect(() => setRouteStats(null), [initialData?.trackingNumber]);
  const handleRouteUpdate = (route) => {
    if (!route) {
      setRouteStats(null);
      return;
    }
    const legs = route.legs || [];
    if (legs.length >= 2) {
      // Waypoints were [pickup, live, destination]: everything up to the
      // last leg is already-driven distance, the last leg is what's left.
      const remainingLeg = legs[legs.length - 1];
      const traveledKm = legs.slice(0, -1).reduce((sum, l) => sum + l.distanceKm, 0);
      setRouteStats({
        traveledKm,
        remainingKm: remainingLeg.distanceKm,
        remainingDurationMin: remainingLeg.durationMin,
        totalKm: route.distanceKm,
      });
    } else {
      // Only pickup->destination (no live GPS ping yet) — nothing driven yet.
      setRouteStats({ traveledKm: 0, remainingKm: route.distanceKm, remainingDurationMin: route.durationMin, totalKm: route.distanceKm });
    }
  };

  useEffect(() => {
    if (!initialData?.trackingNumber) return undefined;
    const unsubscribe = subscribeTopic(`/topic/tracking/${initialData.trackingNumber}`, (msg) => {
      setIsLive(true);
      setData((prev) => ({ ...prev, ...msg }));
    });
    return unsubscribe;
  }, [initialData?.trackingNumber]);

  const status = data.status;
  const delivered = status === 'DELIVERED';
  // Yük hələ götürülməyibsə (trip.startedAt yoxdursa) sayğac başlamamalıdır —
  // dispetçer yükü təyin edən kimi "neçəyə çatacaq" göstərmək mənasızdır,
  // çünki sürücü hələ yola belə çıxmayıb. Sayğac yalnız faktiki götürülmə
  // anından (bax `stages`/PublicTrackingResponse.tripStartedAt) işə düşür.
  const pickedUp = Boolean(data.tripStartedAt);
  // Prefer the real OSRM road-route remaining-time (routeStats) once it's
  // loaded — falls back to the backend's straight-line-based estimate
  // (estimatedEtaMinutes) until then, so the countdown never starts blank.
  const etaMinutesSource = routeStats?.remainingDurationMin ?? data.estimatedEtaMinutes;
  const remaining = useCountdown(etaMinutesSource, !delivered && pickedUp);

  const pickup = data.pickupLatitude != null ? [data.pickupLatitude, data.pickupLongitude] : null;
  const destination = data.destinationLatitude != null ? [data.destinationLatitude, data.destinationLongitude] : null;
  const live = data.lastLatitude != null ? [data.lastLatitude, data.lastLongitude] : null;

  const routePoints = useMemo(() => [pickup, live, destination].filter(Boolean), [pickup, live, destination]);
  const fitPoints = useMemo(() => [pickup, destination].filter(Boolean), [pickup, destination]);

  // stage: 0 = not picked up yet, 1 = en route, 2 = delivered
  const stage = status === 'DELIVERED' ? 2 : status === 'IN_TRANSIT' || status === 'ASSIGNED' ? 1 : 0;

  // "Solda neçə km qət edib / qalıb" — real road km once OSRM has loaded,
  // forced to the fully-driven totals once delivered (last leg should
  // already be ~0 by then, but this keeps it exact and instant).
  const traveledKmDisplay = delivered ? (routeStats?.totalKm ?? null) : (routeStats?.traveledKm ?? null);
  const remainingKmDisplay = delivered ? 0 : (routeStats?.remainingKm ?? null);
  const progressPct = routeStats?.totalKm
    ? Math.min(100, Math.max(0, (traveledKmDisplay / routeStats.totalKm) * 100))
    : delivered ? 100 : null;
  const arrivalClockLabel = !delivered && pickedUp ? formatArrivalClock(etaMinutesSource, i18n.language, t) : null;

  // Addım-addım mərhələ zolağı: sifariş qeydə alınıb -> götürülüb -> yolda
  // -> çatdırılıb, hər addımda əsl vaxt möhürü ilə (bax PublicTrackingResponse:
  // orderCreatedAt/tripStartedAt/tripDeliveredAt). "Yolda" mərhələsinin ayrıca
  // vaxt möhürü yoxdur — hazırda aktivdirsə "Davam edir" göstərilir.
  const activeStageIndex = delivered ? 3 : status === 'IN_TRANSIT' ? 2 : data.tripStartedAt ? 1 : 0;
  const stages = [
    { key: 'created', label: t('tracking.stageCreatedLabel'), time: formatStageTime(data.orderCreatedAt, i18n.language) },
    { key: 'pickedUp', label: t('tracking.stagePickedUpLabel'), sub: data.pickupAddress, time: formatStageTime(data.tripStartedAt, i18n.language) },
    { key: 'transit', label: t('tracking.stageTransitLabel'), sub: null, time: null },
    { key: 'delivered', label: t('tracking.stageDeliveredLabel'), sub: data.destinationAddress, time: formatStageTime(data.tripDeliveredAt, i18n.language) },
  ].map((s, i) => ({
    ...s,
    done: i < activeStageIndex || delivered,
    active: !delivered && i === activeStageIndex,
  }));

  const etaLabel = () => {
    if (delivered) return t('tracking.arrived');
    if (!pickedUp) return t('tracking.pickupAwaited');
    if (remaining == null) return t('tracking.etaUnavailable');
    const h = Math.floor(remaining / 3600);
    const m = Math.floor((remaining % 3600) / 60);
    const s = remaining % 60;
    return h > 0
      ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      : `${m}:${String(s).padStart(2, '0')}`;
  };

  const truckPosition = live || (stage >= 1 ? pickup : null);
  // Memoized so we don't rebuild an <img>-carrying DivIcon (and re-trigger a
  // network fetch of the photo) on every render — only when the driver or
  // their photo actually changes.
  const driverTruckIcon = useMemo(() => buildTruckIcon(data.driverPhotoUrl), [data.driverPhotoUrl]);

  // Sürücünün yüklədiyi nəqliyyat vasitəsi şəkilləri (1 əsas + ən çoxu 4
  // ətraflı, bax DriverController#uploadVehicleMainPhoto/uploadVehicleDetailPhoto)
  // — müştəri kiçik önizləməyə klikləyəndə tam-ekran PhotoLightbox açılır.
  const vehiclePhotoUrls = useMemo(
    () => [data.vehicleMainPhotoUrl, ...(data.vehicleDetailPhotoUrls || [])].filter(Boolean).map((u) => `${API_BASE}${u}`),
    [data.vehicleMainPhotoUrl, data.vehicleDetailPhotoUrls],
  );
  const [vehicleLightboxOpen, setVehicleLightboxOpen] = useState(false);

  // Çatdırılma sübutu (POD) — sürücünün DELIVERED işarələməzdən əvvəl
  // yüklədiyi məcburi foto (bax DriverController#uploadProof,
  // PublicTrackingResponse.proofOfDeliveryUrl). Yalnız çatdırılıb və şəkil
  // varsa göstərilir.
  const podPhotoUrl = data.proofOfDeliveryUrl ? `${API_BASE}${data.proofOfDeliveryUrl}` : null;
  const [podLightboxOpen, setPodLightboxOpen] = useState(false);

  return (
    <div
      className="flex min-h-[560px] flex-col lg:flex-row"
      style={{ border: '1px solid var(--border)', background: 'var(--surface)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow)' }}
    >
      {/* LEFT 30% */}
      <div
        className="flex flex-col gap-4 lg:w-[30%]"
        style={{ padding: 24, borderBottom: '1px solid var(--border)' }}
      >
        <div>
          <div className="flex items-center gap-2">
            <span className="relative flex h-2.5 w-2.5">
              {!delivered && (
                <span
                  className="absolute inline-flex h-full w-full animate-ping rounded-full opacity-60"
                  style={{ background: STATUS_DOT[status] }}
                />
              )}
              <span className="relative inline-flex h-2.5 w-2.5 rounded-full" style={{ background: STATUS_DOT[status] || '#9ca3af' }} />
            </span>
            <span style={{ fontSize: 12, fontWeight: 700, letterSpacing: 0.2, color: 'var(--text)' }}>{t(`status.${status}`)}</span>
            {isLive && (
              <span
                className="ml-1 flex items-center gap-1 rounded-full"
                style={{ padding: '2px 8px', fontSize: 10, fontWeight: 700, background: 'var(--success-bg)', color: 'var(--success)' }}
              >
                <Radio size={10} /> LIVE
              </span>
            )}
          </div>
          <p className="mt-2" style={{ fontSize: 12, color: 'var(--text-muted)' }}>
            {t('tracking.trackingNumberLabel')}: <span style={{ fontFamily: 'monospace', color: 'var(--text)' }}>{data.trackingNumber}</span>
          </p>
          {data.description && <p className="mt-1" style={{ fontSize: 13.5, color: 'var(--text)' }}>{data.description}</p>}
        </div>

        <SidePanel icon={<Clock3 size={12} />} title={t('tracking.eta')}>
          <div
            style={{
              marginTop: 2,
              fontSize: delivered || pickedUp ? 26 : 15,
              fontWeight: 800,
              color: delivered ? 'var(--success)' : !pickedUp ? 'var(--text-muted)' : 'var(--text)',
            }}
          >
            {etaLabel()}
          </div>
          {arrivalClockLabel && (
            <div style={{ marginTop: 2, fontSize: 12, color: 'var(--text-muted)' }}>
              {t('tracking.arrivalEstimateLabel')}: <strong style={{ color: 'var(--text)' }}>{arrivalClockLabel}</strong>
            </div>
          )}

          {(traveledKmDisplay != null || remainingKmDisplay != null) && (
            <>
              <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                <div style={{ flex: 1, textAlign: 'center', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8, padding: '8px 4px' }}>
                  <div style={{ fontSize: 15, fontWeight: 800, color: 'var(--text)' }}>
                    {traveledKmDisplay != null ? traveledKmDisplay.toFixed(0) : '—'} km
                  </div>
                  <div style={{ fontSize: 9.5, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.3, marginTop: 2 }}>
                    {t('tracking.traveledLabel')}
                  </div>
                </div>
                <div style={{ flex: 1, textAlign: 'center', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8, padding: '8px 4px' }}>
                  <div style={{ fontSize: 15, fontWeight: 800, color: 'var(--text)' }}>
                    {remainingKmDisplay != null ? remainingKmDisplay.toFixed(0) : '—'} km
                  </div>
                  <div style={{ fontSize: 9.5, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.3, marginTop: 2 }}>
                    {t('tracking.remainingLabel')}
                  </div>
                </div>
              </div>
              {progressPct != null && (
                <div style={{ marginTop: 8, height: 5, borderRadius: 99, background: 'var(--border)', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      width: `${progressPct}%`,
                      background: delivered ? 'var(--success)' : 'var(--primary)',
                      borderRadius: 99,
                      transition: 'width 0.6s ease',
                    }}
                  />
                </div>
              )}
            </>
          )}
        </SidePanel>

        {/* addım-addım mərhələ zolağı: qəbul -> götürülüb -> yolda -> çatdırılıb,
            hər addımda əsl vaxt möhürü ilə (bax `stages` yuxarıda) */}
        <div className="relative pl-1">
          {stages.map((step, i, arr) => (
            <div key={step.key} className="relative flex gap-3 pb-6 last:pb-0">
              {i < arr.length - 1 && (
                <span
                  className="absolute left-[9px] top-5 h-full w-0.5"
                  style={{ background: step.done ? 'var(--success)' : 'var(--border)' }}
                />
              )}
              <span
                className="relative z-10 mt-0.5 flex h-[19px] w-[19px] shrink-0 items-center justify-center rounded-full"
                style={
                  step.done
                    ? { border: '2px solid var(--success)', background: 'var(--success)', color: '#fff', fontSize: 10, fontWeight: 700 }
                    : step.active
                      ? { border: '2px solid var(--primary)', background: 'var(--primary-bg)', color: 'var(--primary)', fontSize: 10, fontWeight: 700 }
                      : { border: '2px solid var(--border)', background: 'var(--bg)', color: 'var(--text-muted)', fontSize: 10, fontWeight: 700 }
                }
              >
                {step.done ? '✓' : i + 1}
              </span>
              <div className="min-w-0">
                <div style={{ fontSize: 12.5, fontWeight: 600, color: step.done || step.active ? 'var(--text)' : 'var(--text-muted)' }}>
                  {step.label}
                  {step.active && (
                    <span
                      className="ml-1.5 animate-pulse-dot"
                      style={{ display: 'inline-block', width: 6, height: 6, borderRadius: '50%', background: 'var(--primary)', verticalAlign: 'middle' }}
                    />
                  )}
                </div>
                <div className="truncate" style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                  {step.time || (step.active ? t('tracking.stageInProgress') : step.done ? '—' : t('tracking.stageAwaited'))}
                </div>
                {step.sub && <div className="truncate" style={{ fontSize: 11, color: 'var(--text-muted)' }}>{step.sub}</div>}
              </div>
            </div>
          ))}
        </div>

        {/* Çatdırılma sübutu (POD) — bax DriverController#uploadProof */}
        {delivered && podPhotoUrl && (
          <SidePanel icon={<CheckCircle2 size={12} />} title={t('tracking.podTitle')}>
            <button
              type="button"
              onClick={() => setPodLightboxOpen(true)}
              style={{ display: 'block', width: '100%', padding: 0, border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden', cursor: 'pointer', background: 'none' }}
            >
              <img src={podPhotoUrl} alt={t('tracking.podTitle')} style={{ width: '100%', height: 120, objectFit: 'cover', display: 'block' }} />
            </button>
          </SidePanel>
        )}

        {/* driver profile */}
        <SidePanel icon={null} title={t('tracking.driverProfile')} style={{ marginTop: 'auto' }}>
          {data.driverName ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13.5 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div
                  style={{
                    width: 34, height: 34, borderRadius: '50%', overflow: 'hidden', flexShrink: 0,
                    background: '#fff5ea', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    border: '2px solid #fe8704',
                  }}
                >
                  {data.driverPhotoUrl ? (
                    <img src={`${API_BASE}${data.driverPhotoUrl}`} alt={data.driverName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <User size={15} color="#fe8704" />
                  )}
                </div>
                <span style={{ color: 'var(--text)', fontWeight: 600 }}>{data.driverName}</span>
              </div>
              {data.driverPhone && (
                <a href={`tel:${data.driverPhone}`} style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--text)', textDecoration: 'none' }}>
                  <Phone size={14} style={{ color: 'var(--primary)' }} /> {data.driverPhone}
                </a>
              )}
              {data.vehiclePlate && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--text)' }}>
                  <IdCard size={14} style={{ color: 'var(--primary)' }} /> {data.vehiclePlate}
                </div>
              )}
              {vehiclePhotoUrls.length > 0 && (
                <button
                  type="button"
                  onClick={() => setVehicleLightboxOpen(true)}
                  style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'none', border: 'none', padding: 0, cursor: 'pointer', textAlign: 'left' }}
                >
                  <div style={{ width: 30, height: 30, borderRadius: 6, overflow: 'hidden', flexShrink: 0, border: '1px solid var(--border)' }}>
                    <img src={vehiclePhotoUrls[0]} alt={t('tracking.vehiclePhotoAlt')} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  </div>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 12.5, fontWeight: 600, color: 'var(--primary)' }}>
                    <Images size={13} /> {t('tracking.viewVehiclePhotos')}{vehiclePhotoUrls.length > 1 ? ` (${vehiclePhotoUrls.length})` : ''}
                  </span>
                </button>
              )}
            </div>
          ) : (
            <p style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12.5, color: 'var(--text-muted)' }}>
              <PackageSearch size={14} /> {t('tracking.driverNotAssigned')}
            </p>
          )}
        </SidePanel>

        {/* Yol boyu xərclər — reys yaranıbsa göstərilir, şübhəli (anomaly)
            işarələnmiş xərclər ayrıca vurğulanır. Əvvəllər yalnız admin
            görürdü, indi müştəri də öz göndərişinin xərclərini izləyə bilir. */}
        {data.expenses && data.expenses.length > 0 && (
          <SidePanel icon={<Receipt size={12} />} title={t('tracking.expensesTitle')}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {data.expenses.map((exp) => (
                <div
                  key={exp.id}
                  className="flex items-center justify-between"
                  style={{
                    borderRadius: 8,
                    padding: '6px 10px',
                    fontSize: 12,
                    background: exp.isAnomaly ? 'var(--danger-bg)' : 'var(--surface)',
                    border: exp.isAnomaly ? '1px solid #fca5a5' : '1px solid var(--border)',
                  }}
                >
                  <span className="flex min-w-0 items-center gap-1.5 truncate" style={{ color: 'var(--text)' }}>
                    <span>{EXPENSE_CATEGORY_KEY[exp.category]?.icon || '📋'}</span>
                    <span className="truncate">{EXPENSE_CATEGORY_KEY[exp.category] ? t(`driver.${EXPENSE_CATEGORY_KEY[exp.category].key}`) : exp.category}</span>
                    {exp.isAnomaly && (
                      <span
                        className="flex items-center gap-0.5 rounded-full"
                        style={{ padding: '1px 6px', fontSize: 10, fontWeight: 700, background: 'var(--danger-bg)', color: 'var(--danger)' }}
                      >
                        <AlertTriangle size={9} /> {t('tracking.suspiciousBadge')}
                      </span>
                    )}
                  </span>
                  <span className="ml-2 shrink-0" style={{ fontWeight: 700, color: exp.isAnomaly ? 'var(--danger)' : 'var(--text)' }}>
                    {exp.amount} ₼
                  </span>
                </div>
              ))}
            </div>
          </SidePanel>
        )}

        {/* Beynəlxalq göndəriş — yalnız requiresCustoms=true olan
            göndərişlərdə görünür. Sənəd/bəyannamə/sərhəd statusları
            dispetçer panelindən (bax CargoCustomsPanel/TripBorderPanel)
            idarə olunur, müştəri burada yalnız-oxu rejimində izləyir. */}
        {data.requiresCustoms && (
          <SidePanel icon={<Globe2 size={12} />} title={t('tracking.internationalTitle')}>
            <div className="flex flex-wrap gap-1.5" style={{ fontSize: 11, color: 'var(--text)' }}>
              {data.preferredTransportMode && (
                <span style={{ borderRadius: 999, background: 'var(--bg)', padding: '4px 8px' }}>{TRANSPORT_MODE_KEY[data.preferredTransportMode] ? t(`dispatcher.${TRANSPORT_MODE_KEY[data.preferredTransportMode]}`) : data.preferredTransportMode}</span>
              )}
              {data.incoterm && <span style={{ borderRadius: 999, background: 'var(--bg)', padding: '4px 8px' }}>{data.incoterm}</span>}
              {data.originCountry && <span style={{ borderRadius: 999, background: 'var(--bg)', padding: '4px 8px' }}>{data.originCountry} → {data.destinationCountry || '—'}</span>}
              {data.transitCountries && <span style={{ borderRadius: 999, background: 'var(--bg)', padding: '4px 8px' }}>{t('tracking.transitLabel')}: {data.transitCountries}</span>}
            </div>

            {data.documents && data.documents.length > 0 && (
              <div className="mt-3">
                <div className="mb-1.5 flex items-center gap-1.5" style={{ fontSize: 10, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.3, color: 'var(--text-muted)' }}>
                  <FileText size={11} /> {t('tracking.documentsLabel')}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {data.documents.map((d) => (
                    <div key={d.id} className="flex items-center justify-between" style={{ borderRadius: 8, background: 'var(--bg)', padding: '6px 10px', fontSize: 11, color: 'var(--text)' }}>
                      <span className="truncate">{DOCUMENT_KEY[d.type] ? t(`dispatcher.${DOCUMENT_KEY[d.type]}`) : d.type}</span>
                      <span style={{ color: d.status === 'VERIFIED' ? 'var(--success)' : d.status === 'REJECTED' ? 'var(--danger)' : 'var(--text-muted)', fontWeight: 600 }}>
                        {DOCUMENT_STATUS_KEY[d.status] ? t(`tracking.${DOCUMENT_STATUS_KEY[d.status]}`) : d.status}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {data.customsDeclaration && (
              <div className="mt-3">
                <div className="mb-1.5 flex items-center gap-1.5" style={{ fontSize: 10, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.3, color: 'var(--text-muted)' }}>
                  <ShieldCheck size={11} /> {t('dispatcher.declarationTitle')}
                </div>
                <div style={{ borderRadius: 8, background: 'var(--bg)', padding: '8px 10px', fontSize: 11, color: 'var(--text)' }}>
                  <div className="flex items-center justify-between">
                    <span style={{ fontFamily: 'monospace' }}>{data.customsDeclaration.declarationNumber}</span>
                    <span style={{ fontWeight: 600, color: data.customsDeclaration.status === 'CLEARED' ? 'var(--success)' : 'var(--primary)' }}>
                      {DECLARATION_STATUS_KEY[data.customsDeclaration.status] ? t(`tracking.${DECLARATION_STATUS_KEY[data.customsDeclaration.status]}`) : data.customsDeclaration.status}
                    </span>
                  </div>
                  {data.customsDeclaration.totalPayable != null && (
                    <div className="mt-1" style={{ color: 'var(--text-muted)' }}>{t('tracking.dutyVatLabel')}: <span style={{ fontWeight: 700, color: 'var(--text)' }}>{data.customsDeclaration.totalPayable.toFixed(2)} ₼</span></div>
                  )}
                </div>
              </div>
            )}

            {data.borderCrossings && data.borderCrossings.length > 0 && (
              <div className="mt-3">
                <div className="mb-1.5 flex items-center gap-1.5" style={{ fontSize: 10, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.3, color: 'var(--text-muted)' }}>
                  <Flag size={11} /> {t('dispatcher.borderTitle')}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {data.borderCrossings.map((b) => (
                    <div key={b.id} className="flex items-center justify-between" style={{ borderRadius: 8, background: 'var(--bg)', padding: '6px 10px', fontSize: 11, color: 'var(--text)' }}>
                      <span className="truncate">{b.borderPointName}{b.country ? ` (${b.country})` : ''}</span>
                      <span style={{ fontWeight: 600, color: b.customsStatus === 'CLEARED' ? 'var(--success)' : b.customsStatus === 'HELD' ? 'var(--danger)' : 'var(--text-muted)' }}>
                        {BORDER_STATUS_KEY[b.customsStatus] ? t(`dispatcher.${BORDER_STATUS_KEY[b.customsStatus]}`) : b.customsStatus}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </SidePanel>
        )}
      </div>

      {/* RIGHT 70% — map */}
      <div className="relative min-h-[360px] flex-1">
        <MapContainer center={pickup || destination || [40.4093, 49.8671]} zoom={12} style={{ height: '100%', width: '100%' }}>
          <TileLayer attribution="OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <InvalidateSizeOnMount />
          {fitPoints.length > 0 && <FitBounds points={fitPoints} />}
          {routePoints.length > 1 && (
            <RoadRoutePolyline points={routePoints} pathOptions={{ color: '#fe8704', weight: 4, opacity: 0.8 }} onRoute={handleRouteUpdate} />
          )}
          {pickup && <Marker position={pickup} icon={pinStart} />}
          {destination && <Marker position={destination} icon={pinEnd} />}
          {truckPosition && !delivered && <Marker position={truckPosition} icon={driverTruckIcon} />}
        </MapContainer>
        {data.lastUpdatedAt ? (
          <div
            className="absolute bottom-3 left-3"
            style={{ borderRadius: 8, background: 'rgba(255,255,255,0.92)', border: '1px solid #e5e7eb', padding: '6px 12px', fontSize: 11, color: '#374151', backdropFilter: 'blur(4px)', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}
          >
            {t('tracking.lastUpdate')}: {new Date(data.lastUpdatedAt).toLocaleTimeString()}
          </div>
        ) : (
          <div
            className="absolute bottom-3 left-3 flex items-center gap-1.5"
            style={{ borderRadius: 8, background: 'rgba(255,255,255,0.92)', border: '1px solid #e5e7eb', padding: '6px 12px', fontSize: 11, color: 'var(--text-muted)', backdropFilter: 'blur(4px)', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}
          >
            <MapPin size={12} /> {t('tracking.noGpsYet')}
          </div>
        )}
      </div>

      {vehicleLightboxOpen && vehiclePhotoUrls.length > 0 && (
        <PhotoLightbox photos={vehiclePhotoUrls} onClose={() => setVehicleLightboxOpen(false)} />
      )}
      {podLightboxOpen && podPhotoUrl && (
        <PhotoLightbox photos={[podPhotoUrl]} onClose={() => setPodLightboxOpen(false)} />
      )}
    </div>
  );
}
