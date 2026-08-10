// Stage 5 — Driver panel current-trip view. Sourced from
// /api/driver/trips/current/live (see DriverController) so each trip already
// carries destination coordinates for the Waze/Google Maps deep links,
// instead of the bare Trip the old /trips/current returns.
// Restyled to match the rest of the site's light Fleetra theme (.card/.btn/
// .badge/.input) instead of the old dark PWA look — Admin/Dispatcher/
// Customer panels were migrated earlier but Driver was missed.
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigation, MapPin, CheckCircle2, XCircle, LocateFixed, Route as RouteIcon, Flag, MessageCircle, FileText, Bell, AlertTriangle, Wallet, Package, ClipboardCheck } from 'lucide-react';
import { MapContainer, TileLayer, Marker } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import axiosClient from '../../api/axiosClient';
import RestModeCard from '../../components/driver/RestModeCard.jsx';
import RoadRoutePolyline from '../../components/RoadRoutePolyline.jsx';
import OrderChat from '../../components/OrderChat.jsx';
import WaybillView from '../../components/WaybillView.jsx';

const truckIcon = new L.DivIcon({
  className: '',
  html: '<div style="font-size:22px;filter:drop-shadow(0 2px 6px rgba(0,0,0,0.35))">🚛</div>',
  iconSize: [28, 28],
  iconAnchor: [14, 14],
});
const destIcon = new L.DivIcon({
  className: 'custom-pin',
  html: '<div style="width:22px;height:22px;border-radius:50% 50% 50% 0;background:#dc2626;transform:rotate(-45deg);border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.3);"></div>',
  iconSize: [22, 22],
  iconAnchor: [11, 22],
});

// Reys üçün kompakt xəritə — sürücünün son bilinən mövqeyini (ya da hələ
// GPS ping olmayıbsa götürülmə nöqtəsini, bax DriverController#currentTripsLive)
// və təhvil yerini göstərir. Yalnız hər iki koordinat mövcud olanda render olunur.
function DriverTripMap({ trip }) {
  const hasFrom = trip.lastLatitude != null && trip.lastLongitude != null;
  const hasTo = trip.destinationLatitude != null && trip.destinationLongitude != null;
  if (!hasFrom && !hasTo) return null;

  const points = [];
  if (hasFrom) points.push([trip.lastLatitude, trip.lastLongitude]);
  if (hasTo) points.push([trip.destinationLatitude, trip.destinationLongitude]);
  // Hər iki nöqtə varsa ortasından başlayaq ki, xəritə açılanda ikisi də
  // görünsün (MapContainer center yalnız ilk render-də tətbiq olunur).
  const center = hasFrom && hasTo
    ? [(trip.lastLatitude + trip.destinationLatitude) / 2, (trip.lastLongitude + trip.destinationLongitude) / 2]
    : points[0];
  const distanceKm = hasFrom && hasTo
    ? 111 * Math.hypot(trip.lastLatitude - trip.destinationLatitude, (trip.lastLongitude - trip.destinationLongitude) * Math.cos((trip.lastLatitude * Math.PI) / 180))
    : null;
  const zoom = distanceKm == null ? 12 : distanceKm > 250 ? 7 : distanceKm > 80 ? 8.5 : distanceKm > 20 ? 10 : 12;

  return (
    <div style={{ height: 220, borderRadius: 8, overflow: 'hidden', border: '1px solid #e5e7eb', marginTop: 16 }}>
      <MapContainer center={center} zoom={zoom} style={{ height: '100%', width: '100%' }}>
        <TileLayer attribution="OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        {hasFrom && <Marker position={[trip.lastLatitude, trip.lastLongitude]} icon={truckIcon} />}
        {hasTo && <Marker position={[trip.destinationLatitude, trip.destinationLongitude]} icon={destIcon} />}
        {hasFrom && hasTo && (
          <RoadRoutePolyline points={points} pathOptions={{ color: '#fe8704', weight: 3, dashArray: '6 6' }} />
        )}
      </MapContainer>
    </div>
  );
}

const NEXT_STATUS = {
  PLANNED: 'PICKED_UP',
  PICKED_UP: 'IN_TRANSIT',
  IN_TRANSIT: 'DELIVERED',
};
const STATUS_KEY = {
  PLANNED: 'statusPlanned',
  PICKED_UP: 'statusPickedUp',
  IN_TRANSIT: 'statusInTransit',
  DELIVERED: 'statusDelivered',
};
const STATUS_CLASS = {
  PLANNED: 'badge-warning',
  PICKED_UP: 'badge-info',
  IN_TRANSIT: 'badge-info',
  DELIVERED: 'badge-success',
};
const NEXT_ACTION_KEY = {
  PICKED_UP: 'actionPickedUp',
  IN_TRANSIT: 'actionInTransit',
  DELIVERED: 'actionDelivered',
};

// Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — sabit maddə siyahısı, hər
// maddə üçün OK/DEFECT/NA seçilir (bax DriverController#submitDvir).
const DVIR_ITEMS = [
  'BRAKES', 'TIRES', 'LIGHTS', 'MIRRORS', 'HORN', 'WIPERS',
  'FLUID_LEVELS', 'STEERING', 'COUPLING_DEVICE', 'BODY_DAMAGE', 'EMERGENCY_EQUIPMENT',
];

function wazeUrl(lat, lng) {
  return `https://waze.com/ul?ll=${lat},${lng}&navigate=yes`;
}
function googleMapsUrl(lat, lng) {
  return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

export default function DriverCurrentTrip() {
  const { t } = useTranslation();
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  // Reys qəbul/imtina: sürücüyə göndərilib, hələ qəbul/imtina edilməmiş
  // reyslər ayrıca siyahıda — bax DriverController#pendingAcceptanceTrips.
  const [pendingTrips, setPendingTrips] = useState([]);
  const [pendingLoading, setPendingLoading] = useState(true);
  const [actionBusyId, setActionBusyId] = useState(null);
  const [rejectingTripId, setRejectingTripId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [expenseForm, setExpenseForm] = useState({});
  // Yolda fövqəladə hal bildirişi (qəza/sınma/yol bağlanması/digər) — bax
  // DriverController#reportIncident. Foto könüllüdür.
  const [incidentForm, setIncidentForm] = useState({});
  const [incidentOpenId, setIncidentOpenId] = useState(null);
  const [incidentSubmitting, setIncidentSubmitting] = useState(false);
  // Çatdırılma sübutu (POD) — DELIVERED işarələməzdən əvvəl foto məcburidir
  // (bax DriverController#uploadProof). PLANNED→PICKED_UP/PICKED_UP→IN_TRANSIT
  // keçidləri birbaşa qalır, yalnız →DELIVERED bu formanı tələb edir.
  const [podFormOpenId, setPodFormOpenId] = useState(null);
  const [podPhotoFile, setPodPhotoFile] = useState(null);
  const [podSubmitting, setPodSubmitting] = useState(false);
  const [borderForm, setBorderForm] = useState({});
  // Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — bax DVIR_ITEMS sabiti,
  // DriverController#submitDvir/dvirList. dvirDone[tripId] = { PRE_TRIP:
  // bool, POST_TRIP: bool } — forma açılanda server-dən bərpa olunur ki,
  // sürücü artıq doldurduğu yoxlamanı görsün.
  const [dvirOpenId, setDvirOpenId] = useState(null);
  const [dvirForm, setDvirForm] = useState({});
  const [dvirDone, setDvirDone] = useState({});
  const [dvirSubmitting, setDvirSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  // Sürücü panelində indiyədək heç bir yazış imkanı yox idi — müştəri
  // mesaj yazsa belə sürücü tərəfdə görmək/cavablamaq mümkün deyildi (bax
  // ChatService — backend girişi əvvəldən icazə verirdi, sadəcə bu UI
  // əlaqələndirilməmişdi). trip.customers (CustomerSummary, bax
  // LiveTripResponse) hər yükün öz cargoId-sini verir.
  const [openChatCargoId, setOpenChatCargoId] = useState(null);
  const [waybillCargoId, setWaybillCargoId] = useState(null);
  // Dispetçerlə DAXİLİ söhbət — reys üzrə bir toggle (bax aşağıdakı bölmə).
  const [openInternalChat, setOpenInternalChat] = useState(null);

  // Avtomatik GPS ping (fasiləsiz izləmə) — sürücü hər dəfə düyməyə basmaq
  // məcburiyyətində qalmasın deyə, aktiv (PLANNED/PICKED_UP/IN_TRANSIT) hər
  // reys üçün 30 saniyədə bir mövqe avtomatik göndərilir (bax aşağıdakı
  // interval effekti). Əl düyməsi ("Mövqeyimi göndər") fallback olaraq qalır
  // — GPS icazəsi ləngiyəndə/xəritə köhnəlmiş görünəndə sürücü özü də ata bilər.
  const tripsRef = useRef([]);
  useEffect(() => { tripsRef.current = trips; }, [trips]);
  const autoPingStoppedRef = useRef(false);
  // Yalnız göstərmək üçün — məntiq həmişə yuxarıdakı ref-dən oxunur (interval
  // closure-da state stale qala bilər), bu isə sadəcə UI-da badge/mesajı yeniləyir.
  const [autoPingStopped, setAutoPingStopped] = useState(false);

  const load = () => {
    axiosClient.get('/api/driver/trips/current/live')
      .then((res) => setTrips(res.data))
      .finally(() => setLoading(false));
  };

  const loadPending = () => {
    axiosClient.get('/api/driver/trips/pending-acceptance')
      .then((res) => setPendingTrips(res.data))
      .finally(() => setPendingLoading(false));
  };

  // Sürücü qazanc görünürlüyü — bax DriverController#earnings/
  // DriverEarningsSummary. Dəqiq maaş deyil, çatdırılmış reyslərin təxmini
  // xərcinin (Trip.estimatedCost) cəmidir.
  const [earnings, setEarnings] = useState(null);
  const loadEarnings = () => {
    axiosClient.get('/api/driver/earnings')
      .then((res) => setEarnings(res.data))
      .catch(() => setEarnings(null));
  };

  useEffect(() => { load(); loadPending(); loadEarnings(); }, []);

  // Hər 30 saniyədə aktiv reyslərin hamısı üçün sükutla (mesaj göstərmədən)
  // mövqe göndərilir. tripsRef istifadə olunur ki, `trips` dəyişəndə interval
  // yenidən qurulmasın (hər ping sonrası load() → trips yenilənir → əks halda
  // sonsuz interval-yenidənqurma dövrü yaranardı).
  useEffect(() => {
    const interval = setInterval(() => {
      if (autoPingStoppedRef.current) return;
      tripsRef.current.forEach((trip) => {
        if (NEXT_STATUS[trip.status]) sendLocation(trip.tripId, { silent: true });
      });
    }, 30000);
    return () => clearInterval(interval);
  }, []);

  const acceptTrip = async (tripId) => {
    setActionBusyId(tripId);
    try {
      await axiosClient.post(`/api/driver/trips/${tripId}/accept`);
      setMessage(t('driver.tripAccepted'));
      loadPending();
      load();
    } catch {
      setMessage(t('driver.tripAcceptError'));
    } finally {
      setActionBusyId(null);
    }
  };

  const rejectTrip = async (tripId) => {
    setActionBusyId(tripId);
    try {
      await axiosClient.post(`/api/driver/trips/${tripId}/reject`, { reason: rejectReason || null });
      setMessage(t('driver.tripRejected'));
      setRejectingTripId(null);
      setRejectReason('');
      loadPending();
    } catch {
      setMessage(t('driver.tripRejectError'));
    } finally {
      setActionBusyId(null);
    }
  };

  const advanceStatus = async (trip) => {
    const next = NEXT_STATUS[trip.status];
    if (!next) return;
    try {
      await axiosClient.post(`/api/driver/trips/${trip.tripId}/status`, { status: next });
      setMessage(t('driver.statusUpdated', { status: t(`driver.${STATUS_KEY[next]}`) }));
      load();
    } catch {
      setMessage(t('driver.statusUpdateError'));
    }
  };

  // Çatdırıldığı işarələnməzdən əvvəl foto yükləyir (bax
  // DriverController#uploadProof), sonra statusu DELIVERED-ə keçirir.
  // Fotosuz DELIVERED-ə keçid mümkün deyil — düymə yalnız bu formanı açır,
  // birbaşa advanceStatus çağırmır (bax aşağıdakı render).
  const submitDeliveryProof = async (trip) => {
    if (!podPhotoFile) {
      setMessage(t('driver.podPhotoRequired'));
      return;
    }
    setPodSubmitting(true);
    try {
      const fd = new FormData();
      fd.append('photo', podPhotoFile);
      await axiosClient.post(`/api/driver/trips/${trip.tripId}/proof`, fd, {
        headers: { 'Content-Type': undefined },
      });
      await advanceStatus(trip);
      setPodFormOpenId(null);
      setPodPhotoFile(null);
    } catch {
      setMessage(t('driver.podUploadError'));
    } finally {
      setPodSubmitting(false);
    }
  };

  const sendLocation = (tripId, opts = {}) => {
    const silent = opts.silent === true;
    // Brauzerlər Geolocation API-ni yalnız "təhlükəsiz kontekst"də (HTTPS
    // və ya localhost) buraxır — LAN IP üzərindən sadə http:// ilə açılan
    // saytda (məs. telefonla test edərkən) bu API tamam örtülü qalır və
    // heç bir xəta da vermədən sükutla işləməz. Bunu əvvəlcədən yoxlayıb
    // aydın mesaj göstəririk, əvəzinə "heç nə baş vermir" hissi yaranmasın.
    // (Avtomatik/silent ping-lərdə bu yoxlamalar mesaj göstərmədən sadəcə
    // dayandırır — hər 30 saniyədə eyni xəbərdarlığı təkrarlamasın deyə.)
    if (!window.isSecureContext) {
      if (!silent) setMessage(t('driver.geoHttpsRequired'));
      return;
    }
    if (!navigator.geolocation) {
      if (!silent) setMessage(t('driver.geoNotSupported'));
      return;
    }
    if (!silent) setMessage(t('driver.geoFetching'));
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          await axiosClient.post(`/api/driver/trips/${tripId}/tracking`, {
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
          });
          if (!silent) setMessage(t('driver.geoSent'));
          load();
        } catch {
          if (!silent) setMessage(t('driver.geoSendError'));
        }
      },
      (err) => {
        if (silent) {
          // İcazə rədd edilibsə avtomatik ping-i tamam dayandırırıq (əks
          // halda hər 30 saniyədə eyni brauzer xətası sükutla təkrarlanardı)
          // və sürücüyə BİR DƏFƏ xəbər veririk ki, əl düyməsindən istifadə etsin.
          if (err.code === 1) {
            autoPingStoppedRef.current = true;
            setAutoPingStopped(true);
            setMessage(t('driver.geoAutoStopped'));
          }
          return;
        }
        // Əvvəlki versiyada timeout təyin olunmamışdı (default = sonsuz) —
        // GPS siqnalı tapılmayanda getCurrentPosition nə uğur, nə də xəta
        // callback-i çağırmadan əbədi gözləyirdi, düymə "işləmirmiş" kimi
        // görünürdü. İndi 10 saniyədən sonra aydın xəta ilə geri qayıdır.
        const ERROR_MESSAGES = {
          1: t('driver.geoErrorPermission'),
          2: t('driver.geoErrorUnavailable'),
          3: t('driver.geoErrorTimeout'),
        };
        setMessage(ERROR_MESSAGES[err.code] || t('driver.geoErrorGeneric'));
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  };

  const handleExpenseChange = (tripId, field, value) => {
    setExpenseForm((prev) => ({ ...prev, [tripId]: { ...prev[tripId], [field]: value } }));
  };

  const submitExpense = async (tripId) => {
    const form = expenseForm[tripId] || {};
    if (!form.category || !form.amount) {
      setMessage(t('driver.expenseCategoryRequired'));
      return;
    }
    try {
      // Qəbz fotosu könüllüdür — bax DriverController#addExpenseWithReceipt
      // (multipart). Foto seçilməyəndə də eyni endpoint işləyir, sadəcə
      // receiptPhotoUrl null qalır.
      const fd = new FormData();
      fd.append('category', form.category);
      fd.append('amount', parseFloat(form.amount));
      fd.append('description', form.description || '');
      if (form.receiptPhoto) fd.append('photo', form.receiptPhoto);
      const res = await axiosClient.post(`/api/driver/trips/${tripId}/expenses`, fd, {
        headers: { 'Content-Type': undefined },
      });
      setMessage(res.data.isAnomaly ? t('driver.expenseRecordedAnomaly') : t('driver.expenseRecorded'));
      setExpenseForm((prev) => ({ ...prev, [tripId]: {} }));
    } catch {
      setMessage(t('driver.expenseError'));
    }
  };

  const handleBorderChange = (tripId, field, value) => {
    setBorderForm((prev) => ({ ...prev, [tripId]: { ...prev[tripId], [field]: value } }));
  };

  // Tranzit zamanı (çoxölkəli marşrutda) sürücü hər sərhəd/gömrük
  // məntəqəsindən keçdiyini özü qeyd edə bilsin — bax DriverController
  // #addBorderCrossing / BorderCrossing entity. Dispetçerin bunu əl ilə
  // daxil etməsini gözləməyə ehtiyac qalmır.
  const submitBorderCrossing = async (tripId) => {
    const form = borderForm[tripId] || {};
    if (!form.borderPointName) {
      setMessage(t('driver.borderNameRequired'));
      return;
    }
    try {
      await axiosClient.post(`/api/driver/trips/${tripId}/border-crossings`, {
        borderPointName: form.borderPointName,
        country: form.country || '',
        customsStatus: 'CLEARED',
      });
      setMessage(t('driver.borderRecorded'));
      setBorderForm((prev) => ({ ...prev, [tripId]: {} }));
    } catch {
      setMessage(t('driver.borderError'));
    }
  };

  const handleIncidentChange = (tripId, field, value) => {
    setIncidentForm((prev) => ({ ...prev, [tripId]: { ...prev[tripId], [field]: value } }));
  };

  // Yolda fövqəladə hal bildirişi — foto könüllüdür, tip məcburidir (bax
  // DriverController#reportIncident, NotificationService#notifyIncidentReported).
  const submitIncident = async (tripId) => {
    const form = incidentForm[tripId] || {};
    if (!form.type) {
      setMessage(t('driver.incidentTypeRequired'));
      return;
    }
    setIncidentSubmitting(true);
    try {
      const fd = new FormData();
      fd.append('type', form.type);
      if (form.description) fd.append('description', form.description);
      if (form.photo) fd.append('photo', form.photo);
      await axiosClient.post(`/api/driver/trips/${tripId}/incidents`, fd, {
        headers: { 'Content-Type': undefined },
      });
      setMessage(t('driver.incidentReported'));
      setIncidentForm((prev) => ({ ...prev, [tripId]: {} }));
      setIncidentOpenId(null);
    } catch {
      setMessage(t('driver.incidentError'));
    } finally {
      setIncidentSubmitting(false);
    }
  };

  // Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — forma açılanda server-dən
  // bu reys üçün artıq doldurulmuş yoxlamaları (PRE_TRIP/POST_TRIP) çəkirik
  // ki, sürücü təkrar doldurmasın (badge göstərilir, amma bloklanmır).
  const openDvir = (tripId) => {
    setDvirOpenId(tripId);
    if (!dvirForm[tripId]) {
      setDvirForm((prev) => ({ ...prev, [tripId]: { type: 'PRE_TRIP', items: {}, notes: '' } }));
    }
    axiosClient.get(`/api/driver/trips/${tripId}/dvir`)
      .then((res) => {
        const done = { PRE_TRIP: false, POST_TRIP: false };
        (res.data || []).forEach((insp) => { done[insp.type] = true; });
        setDvirDone((prev) => ({ ...prev, [tripId]: done }));
      })
      .catch(() => {});
  };

  const handleDvirTypeChange = (tripId, type) => {
    setDvirForm((prev) => ({ ...prev, [tripId]: { ...(prev[tripId] || { items: {}, notes: '' }), type } }));
  };

  const handleDvirItemChange = (tripId, itemKey, status) => {
    setDvirForm((prev) => ({
      ...prev,
      [tripId]: {
        ...(prev[tripId] || { type: 'PRE_TRIP', items: {}, notes: '' }),
        items: { ...(prev[tripId]?.items || {}), [itemKey]: status },
      },
    }));
  };

  const handleDvirNotesChange = (tripId, notes) => {
    setDvirForm((prev) => ({ ...prev, [tripId]: { ...(prev[tripId] || { type: 'PRE_TRIP', items: {} }), notes } }));
  };

  const submitDvir = async (tripId) => {
    const form = dvirForm[tripId] || { type: 'PRE_TRIP', items: {}, notes: '' };
    if (!form.items || Object.keys(form.items).length < DVIR_ITEMS.length) {
      setMessage(t('driver.dvirIncompleteError'));
      return;
    }
    setDvirSubmitting(true);
    try {
      const res = await axiosClient.post(`/api/driver/trips/${tripId}/dvir`, {
        type: form.type,
        items: form.items,
        notes: form.notes || null,
      });
      setMessage(res.data.hasDefects ? t('driver.dvirSubmittedDefect') : t('driver.dvirSubmitted'));
      setDvirDone((prev) => ({ ...prev, [tripId]: { ...(prev[tripId] || {}), [form.type]: true } }));
      setDvirForm((prev) => ({ ...prev, [tripId]: { type: form.type, items: {}, notes: '' } }));
      setDvirOpenId(null);
    } catch {
      setMessage(t('driver.dvirSubmitError'));
    } finally {
      setDvirSubmitting(false);
    }
  };

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <h2>{t('driver.currentTripTitle')}</h2>
      <p>{t('driver.currentTripDesc')}</p>

      {message && <div className="alert alert-success mt-16">{message}</div>}

      {/* Qazanc görünürlüyü — bax DriverController#earnings. Dəqiq maaş
          deyil, çatdırılmış reyslərin təxmini xərcinin cəmidir, ona görə
          "təxmini" sözü ilə qeyd olunur. */}
      {earnings && (
        <div className="grid grid-2 mt-16" style={{ gap: 12 }}>
          <div className="card" style={{ padding: 14 }}>
            <div className="flex items-center gap-1.5 text-xs text-muted">
              <Package size={13} style={{ color: 'var(--primary)' }} /> {t('driver.tripsThisMonth')}
            </div>
            <h3 style={{ margin: '6px 0 0' }}>{earnings.tripsThisMonth}</h3>
          </div>
          <div className="card" style={{ padding: 14 }}>
            <div className="flex items-center gap-1.5 text-xs text-muted">
              <Wallet size={13} style={{ color: 'var(--success)' }} /> {t('driver.earningsThisMonth')}
            </div>
            <h3 style={{ margin: '6px 0 0', color: 'var(--success)' }}>{earnings.earningsThisMonth.toFixed(2)} ₼</h3>
          </div>
        </div>
      )}
      {earnings && (
        <p className="text-muted mt-8" style={{ fontSize: 11.5 }}>
          {t('driver.earningsTotalNote', { trips: earnings.tripsTotal, amount: earnings.earningsTotal.toFixed(2) })}
        </p>
      )}

      {!pendingLoading && pendingTrips.length > 0 && (
        <div className="mt-16">
          <h3 className="flex items-center gap-1.5" style={{ fontSize: 15, margin: '0 0 10px' }}>
            <Bell size={15} style={{ color: 'var(--warning)' }} /> {t('driver.pendingTripsTitle')}
          </h3>
          {pendingTrips.map((trip) => (
            <div
              className="card mt-8"
              key={trip.tripId}
              style={{ border: '1px solid var(--warning)', background: 'var(--warning-bg)' }}
            >
              <div className="flex-between" style={{ alignItems: 'flex-start' }}>
                <div style={{ minWidth: 0 }}>
                  <div className="flex items-center gap-1.5 text-xs text-muted">
                    <RouteIcon size={12} /> {t('driver.pendingTripCardLabel', { id: trip.tripId })}
                  </div>
                  {trip.pickupAddress && trip.destinationAddress && (
                    <p style={{ margin: '4px 0 0', display: 'flex', alignItems: 'center', gap: 5, fontSize: 13.5, fontWeight: 600 }}>
                      <MapPin size={13} style={{ color: 'var(--primary)', flexShrink: 0 }} />
                      {t('driver.pendingTripRoute', { pickup: trip.pickupAddress, destination: trip.destinationAddress })}
                    </p>
                  )}
                </div>
                <span className="badge badge-warning" style={{ flexShrink: 0 }}>{t('driver.statusPendingAcceptance')}</span>
              </div>

              {rejectingTripId === trip.tripId ? (
                <div className="mt-16" style={{ border: '1px solid #e5e7eb', borderRadius: 10, padding: 14, background: '#fff' }}>
                  <p style={{ margin: '0 0 8px', fontWeight: 600, fontSize: 13.5 }}>{t('driver.confirmRejectTitle')}</p>
                  <p className="text-muted" style={{ margin: '0 0 10px', fontSize: 12.5 }}>{t('driver.confirmRejectDesc')}</p>
                  <input
                    className="input"
                    placeholder={t('driver.rejectReasonPlaceholder')}
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                  />
                  <div className="grid grid-2 mt-8">
                    <button
                      type="button"
                      className="btn"
                      onClick={() => { setRejectingTripId(null); setRejectReason(''); }}
                      style={{ justifyContent: 'center' }}
                    >
                      {t('common.cancel')}
                    </button>
                    <button
                      type="button"
                      className="btn"
                      disabled={actionBusyId === trip.tripId}
                      onClick={() => rejectTrip(trip.tripId)}
                      style={{ justifyContent: 'center', gap: 6, background: 'var(--danger)', borderColor: 'var(--danger)', color: '#fff' }}
                    >
                      <XCircle size={15} /> {t('driver.confirmRejectBtn')}
                    </button>
                  </div>
                </div>
              ) : (
                <div className="grid grid-2 mt-16">
                  <button
                    type="button"
                    disabled={actionBusyId === trip.tripId}
                    onClick={() => acceptTrip(trip.tripId)}
                    className="btn"
                    style={{ justifyContent: 'center', gap: 6, background: 'var(--success)', borderColor: 'var(--success)', color: '#fff' }}
                  >
                    <CheckCircle2 size={16} /> {t('driver.acceptTripBtn')}
                  </button>
                  <button
                    type="button"
                    disabled={actionBusyId === trip.tripId}
                    onClick={() => setRejectingTripId(trip.tripId)}
                    className="btn"
                    style={{ justifyContent: 'center', gap: 6 }}
                  >
                    <XCircle size={16} /> {t('driver.rejectTripBtn')}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {trips.length === 0 && pendingTrips.length === 0 && !pendingLoading && (
        <div className="card mt-16 text-center text-muted">{t('driver.noActiveTrip')}</div>
      )}

      {trips.map((trip) => (
        <div className="card hover-lift mt-16" key={trip.tripId}>
          <div className="flex-between" style={{ alignItems: 'flex-start' }}>
            <div style={{ minWidth: 0 }}>
              <div className="flex items-center gap-1.5 text-xs text-muted">
                <RouteIcon size={12} /> {t('driver.tripLabel')}{trip.tripId}
              </div>
              <h3 style={{ margin: '4px 0 0' }}>{trip.routeInfo || '—'}</h3>
              {trip.destinationAddress && (
                <p className="text-muted" style={{ margin: '4px 0 0', display: 'flex', alignItems: 'center', gap: 5, fontSize: 13 }}>
                  <MapPin size={13} style={{ color: 'var(--primary)', flexShrink: 0 }} /> {trip.destinationAddress}
                </p>
              )}
            </div>
            <span className={`badge ${STATUS_CLASS[trip.status] || 'badge-info'}`} style={{ flexShrink: 0 }}>
              {STATUS_KEY[trip.status] ? t(`driver.${STATUS_KEY[trip.status]}`) : trip.status}
            </span>
          </div>

          <DriverTripMap trip={trip} />

          {/* Navigation deep-links */}
          {trip.destinationLatitude != null && trip.destinationLongitude != null && (
            <div className="grid grid-2 mt-16">
              <a
                href={wazeUrl(trip.destinationLatitude, trip.destinationLongitude)}
                target="_blank"
                rel="noreferrer"
                className="btn btn-primary"
                style={{ justifyContent: 'center', gap: 6 }}
              >
                <Navigation size={15} /> {t('driver.navigateWaze')}
              </a>
              <a
                href={googleMapsUrl(trip.destinationLatitude, trip.destinationLongitude)}
                target="_blank"
                rel="noreferrer"
                className="btn"
                style={{ justifyContent: 'center', gap: 6 }}
              >
                <Navigation size={15} /> {t('driver.navigateGoogle')}
              </a>
            </div>
          )}

          <div className="grid grid-2 mt-16">
            {NEXT_STATUS[trip.status] && (
              <button
                type="button"
                onClick={() => {
                  // DELIVERED-ə keçid üçün əvvəlcə foto tələb olunur (bax
                  // submitDeliveryProof) — birbaşa advanceStatus çağırmaq
                  // əvəzinə formanı açırıq.
                  if (NEXT_STATUS[trip.status] === 'DELIVERED') {
                    setPodFormOpenId(trip.tripId);
                  } else {
                    advanceStatus(trip);
                  }
                }}
                className="btn"
                style={{ justifyContent: 'center', gap: 6, background: 'var(--success)', borderColor: 'var(--success)', color: '#fff' }}
              >
                <CheckCircle2 size={16} /> {t(`driver.${NEXT_ACTION_KEY[NEXT_STATUS[trip.status]]}`)}
              </button>
            )}
            <button
              type="button"
              onClick={() => sendLocation(trip.tripId)}
              className="btn"
              style={{ justifyContent: 'center', gap: 6 }}
            >
              <LocateFixed size={15} /> {t('driver.sendLocation')}
            </button>
          </div>

          {/* Fasiləsiz izləmə göstəricisi — hər 30 saniyədə mövqe avtomatik
              göndərilir (bax yuxarıdakı interval effekti), əl düyməsi yalnız
              fallback kimi qalır. */}
          {NEXT_STATUS[trip.status] && (
            <p className="text-muted mt-8" style={{ fontSize: 11.5, display: 'flex', alignItems: 'center', gap: 5 }}>
              <LocateFixed size={11} style={{ color: autoPingStopped ? 'var(--danger)' : 'var(--success)' }} />
              {autoPingStopped ? t('driver.geoAutoStopped') : t('driver.geoAutoTrackingOn')}
            </p>
          )}

          {/* Çatdırılma sübutu (POD) — DELIVERED işarələməzdən əvvəl foto
              məcburidir (bax DriverController#uploadProof/submitDeliveryProof). */}
          {podFormOpenId === trip.tripId && (
            <div className="mt-16" style={{ border: '1px solid var(--border)', borderRadius: 10, padding: 14, background: 'var(--bg)' }}>
              <h4 className="flex items-center gap-1.5" style={{ margin: '0 0 10px', fontSize: 14 }}>
                <CheckCircle2 size={14} style={{ color: 'var(--success)' }} /> {t('driver.podFormTitle')}
              </h4>
              <input
                type="file"
                accept="image/*"
                className="input"
                onChange={(e) => setPodPhotoFile(e.target.files?.[0] || null)}
              />
              <div className="grid grid-2 mt-8">
                <button
                  type="button"
                  className="btn"
                  onClick={() => { setPodFormOpenId(null); setPodPhotoFile(null); }}
                  disabled={podSubmitting}
                  style={{ justifyContent: 'center' }}
                >
                  {t('common.cancel')}
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => submitDeliveryProof(trip)}
                  disabled={podSubmitting}
                  style={{ justifyContent: 'center', gap: 6, background: 'var(--success)', borderColor: 'var(--success)', color: '#fff' }}
                >
                  <CheckCircle2 size={15} /> {podSubmitting ? t('common.loading') : t('driver.podSubmitBtn')}
                </button>
              </div>
            </div>
          )}

          {/* Fövqəladə hal bildirişi — yolda qəza/sınma/yol bağlanması və s.
              baş verəndə dispetçer/admin dərhal xəbərdar olsun deyə (bax
              DriverController#reportIncident). */}
          <div className="mt-16">
            {incidentOpenId === trip.tripId ? (
              <div style={{ border: '1px solid rgba(220,38,38,0.35)', background: 'var(--danger-bg)', borderRadius: 10, padding: 14 }}>
                <h4 className="flex items-center gap-1.5" style={{ margin: '0 0 10px', fontSize: 14, color: 'var(--danger)' }}>
                  <AlertTriangle size={14} /> {t('driver.incidentFormTitle')}
                </h4>
                <select
                  className="input"
                  value={incidentForm[trip.tripId]?.type || ''}
                  onChange={(e) => handleIncidentChange(trip.tripId, 'type', e.target.value)}
                >
                  <option value="">{t('driver.incidentTypePlaceholder')}</option>
                  <option value="ACCIDENT">{t('driver.incidentAccident')}</option>
                  <option value="BREAKDOWN">{t('driver.incidentBreakdown')}</option>
                  <option value="ROAD_CLOSURE">{t('driver.incidentRoadClosure')}</option>
                  <option value="OTHER">{t('driver.incidentOther')}</option>
                </select>
                <textarea
                  className="input mt-8"
                  rows={3}
                  placeholder={t('driver.incidentDescPlaceholder')}
                  value={incidentForm[trip.tripId]?.description || ''}
                  onChange={(e) => handleIncidentChange(trip.tripId, 'description', e.target.value)}
                />
                <input
                  type="file"
                  accept="image/*"
                  className="input mt-8"
                  onChange={(e) => handleIncidentChange(trip.tripId, 'photo', e.target.files?.[0] || null)}
                />
                <div className="grid grid-2 mt-8">
                  <button
                    type="button"
                    className="btn"
                    onClick={() => { setIncidentOpenId(null); setIncidentForm((prev) => ({ ...prev, [trip.tripId]: {} })); }}
                    disabled={incidentSubmitting}
                    style={{ justifyContent: 'center' }}
                  >
                    {t('common.cancel')}
                  </button>
                  <button
                    type="button"
                    className="btn"
                    onClick={() => submitIncident(trip.tripId)}
                    disabled={incidentSubmitting}
                    style={{ justifyContent: 'center', gap: 6, background: 'var(--danger)', borderColor: 'var(--danger)', color: '#fff' }}
                  >
                    <AlertTriangle size={15} /> {incidentSubmitting ? t('common.loading') : t('driver.incidentSubmitBtn')}
                  </button>
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setIncidentOpenId(trip.tripId)}
                className="btn btn-block"
                style={{ justifyContent: 'center', gap: 6, color: 'var(--danger)', borderColor: 'var(--danger)' }}
              >
                <AlertTriangle size={15} /> {t('driver.incidentReportBtn')}
              </button>
            )}
          </div>

          {/* Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — bax DVIR_ITEMS
              sabiti, DriverController#submitDvir. Bloklayıcı deyil (POD-dan
              fərqli olaraq statusu keçidi tələb etmir), sadəcə görünürlük
              üçündür — defekt qeyd olunarsa dispetçer/admin bildiriş alır. */}
          <div className="mt-16">
            {dvirOpenId === trip.tripId ? (
              <div style={{ border: '1px solid var(--border)', background: 'var(--bg)', borderRadius: 10, padding: 14 }}>
                <h4 className="flex items-center gap-1.5" style={{ margin: '0 0 10px', fontSize: 14 }}>
                  <ClipboardCheck size={14} style={{ color: 'var(--primary)' }} /> {t('driver.dvirFormTitle')}
                </h4>

                <div className="grid grid-2" style={{ gap: 8 }}>
                  <button
                    type="button"
                    onClick={() => handleDvirTypeChange(trip.tripId, 'PRE_TRIP')}
                    className="btn btn-sm"
                    style={{
                      justifyContent: 'center',
                      background: (dvirForm[trip.tripId]?.type || 'PRE_TRIP') === 'PRE_TRIP' ? 'var(--primary)' : undefined,
                      color: (dvirForm[trip.tripId]?.type || 'PRE_TRIP') === 'PRE_TRIP' ? '#fff' : undefined,
                    }}
                  >
                    {t('driver.dvirPreTrip')}{dvirDone[trip.tripId]?.PRE_TRIP ? ' ✓' : ''}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDvirTypeChange(trip.tripId, 'POST_TRIP')}
                    className="btn btn-sm"
                    style={{
                      justifyContent: 'center',
                      background: dvirForm[trip.tripId]?.type === 'POST_TRIP' ? 'var(--primary)' : undefined,
                      color: dvirForm[trip.tripId]?.type === 'POST_TRIP' ? '#fff' : undefined,
                    }}
                  >
                    {t('driver.dvirPostTrip')}{dvirDone[trip.tripId]?.POST_TRIP ? ' ✓' : ''}
                  </button>
                </div>

                <div className="mt-8" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {DVIR_ITEMS.map((item) => (
                    <div key={item} className="flex-between" style={{ fontSize: 12.5, gap: 8, flexWrap: 'wrap' }}>
                      <span>{t(`driver.dvirItem_${item}`)}</span>
                      <div className="flex items-center gap-1.5">
                        {['OK', 'DEFECT', 'NA'].map((status) => {
                          const selected = dvirForm[trip.tripId]?.items?.[item] === status;
                          const activeColor = status === 'DEFECT' ? 'var(--danger)' : status === 'OK' ? 'var(--success)' : 'var(--text-muted)';
                          return (
                            <button
                              key={status}
                              type="button"
                              onClick={() => handleDvirItemChange(trip.tripId, item, status)}
                              className="btn btn-sm"
                              style={{
                                padding: '3px 8px',
                                fontSize: 11,
                                background: selected ? activeColor : undefined,
                                color: selected ? '#fff' : undefined,
                                borderColor: status === 'DEFECT' ? 'var(--danger)' : status === 'OK' ? 'var(--success)' : undefined,
                              }}
                            >
                              {t(`driver.dvirStatus${status === 'OK' ? 'Ok' : status === 'DEFECT' ? 'Defect' : 'Na'}`)}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>

                <textarea
                  className="input mt-8"
                  rows={2}
                  placeholder={t('driver.dvirNotesPlaceholder')}
                  value={dvirForm[trip.tripId]?.notes || ''}
                  onChange={(e) => handleDvirNotesChange(trip.tripId, e.target.value)}
                />

                <div className="grid grid-2 mt-8">
                  <button
                    type="button"
                    className="btn"
                    onClick={() => setDvirOpenId(null)}
                    disabled={dvirSubmitting}
                    style={{ justifyContent: 'center' }}
                  >
                    {t('common.cancel')}
                  </button>
                  <button
                    type="button"
                    className="btn"
                    onClick={() => submitDvir(trip.tripId)}
                    disabled={dvirSubmitting}
                    style={{ justifyContent: 'center', gap: 6, background: 'var(--primary)', borderColor: 'var(--primary)', color: '#fff' }}
                  >
                    <ClipboardCheck size={15} /> {dvirSubmitting ? t('common.loading') : t('driver.dvirSubmitBtn')}
                  </button>
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => openDvir(trip.tripId)}
                className="btn btn-block"
                style={{ justifyContent: 'center', gap: 6 }}
              >
                <ClipboardCheck size={15} /> {t('driver.dvirOpenBtn')}
              </button>
            )}
          </div>

          {/* Müştəri(lər)lə yazış — bir reysə bir neçə yük birləşdirilə
              bildiyi üçün hər biri üçün ayrıca düymə/otaq (bax CustomerInfoModal
              eyni naxış). */}
          {trip.customers && trip.customers.length > 0 && (
            <div className="mt-16" style={{ border: '1px solid var(--border)', borderRadius: 10, padding: 14, background: 'var(--bg)' }}>
              <h4 className="flex items-center gap-1.5" style={{ margin: '0 0 10px', fontSize: 14 }}>
                <MessageCircle size={14} style={{ color: 'var(--primary)' }} /> {t('driver.customerChatTitle')}
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {trip.customers.map((c, i) => (
                  <div key={c.cargoId || i}>
                    <div className="flex-between" style={{ alignItems: 'center' }}>
                      <span style={{ fontSize: 13 }}>
                        {c.fullName || t('driver.customerFallback')}{c.trackingNumber ? ` — ${c.trackingNumber}` : ''}
                      </span>
                      {c.cargoId && (
                        <div className="flex items-center gap-1.5">
                          <button
                            type="button"
                            className="btn btn-sm flex items-center gap-1.5"
                            onClick={() => setWaybillCargoId(c.cargoId)}
                          >
                            <FileText size={12} /> {t('driver.waybillBtn')}
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm flex items-center gap-1.5"
                            onClick={() => setOpenChatCargoId(openChatCargoId === c.cargoId ? null : c.cargoId)}
                          >
                            <MessageCircle size={12} /> {openChatCargoId === c.cargoId ? t('driver.closeChatBtn') : t('driver.chatBtn')}
                          </button>
                        </div>
                      )}
                    </div>
                    {openChatCargoId === c.cargoId && (
                      <div className="mt-8">
                        <OrderChat cargoId={c.cargoId} channel="CUSTOMER_DRIVER" />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Dispetçerlə DAXİLİ yazış — müştəri görmür (bax
              entity/ChatChannel). Reys üzrə bir otaq kifayətdir, "otaq
              açarı" kimi reysin birinci yükünün cargoId-si istifadə olunur
              (bax ChatService — INTERNAL otağı da cargoId ilə açılır). */}
          {trip.customers && trip.customers.length > 0 && trip.customers[0].cargoId && (
            <div className="mt-16" style={{ border: '1px solid var(--border)', borderRadius: 10, padding: 14, background: 'var(--bg)' }}>
              <div className="flex-between" style={{ alignItems: 'center' }}>
                <h4 className="flex items-center gap-1.5" style={{ margin: 0, fontSize: 14 }}>
                  <MessageCircle size={14} style={{ color: 'var(--primary)' }} /> {t('driver.dispatcherChatTitle')}
                </h4>
                <button
                  type="button"
                  className="btn btn-sm flex items-center gap-1.5"
                  onClick={() => setOpenInternalChat((prev) => (prev === trip.tripId ? null : trip.tripId))}
                >
                  <MessageCircle size={12} /> {openInternalChat === trip.tripId ? t('driver.closeChatBtn') : t('driver.chatBtn')}
                </button>
              </div>
              {openInternalChat === trip.tripId && (
                <div className="mt-8">
                  <OrderChat cargoId={trip.customers[0].cargoId} channel="INTERNAL" />
                </div>
              )}
            </div>
          )}

          {/* Stage 5 — Rest Mode fatigue tracker */}
          <div className="mt-16">
            <RestModeCard tripId={trip.tripId} />
          </div>

          {/* Expense form */}
          <div className="mt-16" style={{ border: '1px solid #e5e7eb', borderRadius: 10, padding: 14, background: '#f9fafb' }}>
            <h4 style={{ margin: '0 0 10px', fontSize: 14 }}>{t('driver.expenseAddTitle')}</h4>
            <div className="grid grid-2" style={{ gap: 10 }}>
              <select
                className="input"
                value={expenseForm[trip.tripId]?.category || ''}
                onChange={(e) => handleExpenseChange(trip.tripId, 'category', e.target.value)}
              >
                <option value="">{t('driver.categoryPlaceholder')}</option>
                <option value="FUEL">{t('driver.categoryFuel')}</option>
                <option value="TOLL">{t('driver.categoryToll')}</option>
                <option value="FOOD">{t('driver.categoryFood')}</option>
                <option value="MAINTENANCE">{t('driver.categoryMaintenance')}</option>
                <option value="OTHER">{t('driver.categoryOther')}</option>
              </select>
              <input
                className="input"
                placeholder={t('driver.amountPlaceholder')}
                value={expenseForm[trip.tripId]?.amount || ''}
                onChange={(e) => handleExpenseChange(trip.tripId, 'amount', e.target.value)}
              />
            </div>
            <input
              className="input mt-8"
              placeholder={t('driver.descriptionPlaceholder')}
              value={expenseForm[trip.tripId]?.description || ''}
              onChange={(e) => handleExpenseChange(trip.tripId, 'description', e.target.value)}
            />
            {/* Qəbz fotosu — könüllüdür, bax DriverController#addExpenseWithReceipt */}
            <input
              type="file"
              accept="image/*"
              className="input mt-8"
              onChange={(e) => handleExpenseChange(trip.tripId, 'receiptPhoto', e.target.files?.[0] || null)}
            />
            <button className="btn btn-primary btn-sm mt-8" onClick={() => submitExpense(trip.tripId)}>{t('driver.submitExpenseBtn')}</button>
          </div>

          {/* Sərhəd keçidi (tranzit reyslər üçün) */}
          <div className="mt-16" style={{ border: '1px solid #e5e7eb', borderRadius: 10, padding: 14, background: '#f9fafb' }}>
            <h4 className="flex items-center gap-1.5" style={{ margin: '0 0 10px', fontSize: 14 }}>
              <Flag size={14} style={{ color: 'var(--primary)' }} /> {t('driver.borderCrossingTitle')}
            </h4>
            <div className="grid grid-2" style={{ gap: 10 }}>
              <input
                className="input"
                placeholder={t('driver.borderPointPlaceholder')}
                value={borderForm[trip.tripId]?.borderPointName || ''}
                onChange={(e) => handleBorderChange(trip.tripId, 'borderPointName', e.target.value)}
              />
              <input
                className="input"
                placeholder={t('driver.countryPlaceholder')}
                value={borderForm[trip.tripId]?.country || ''}
                onChange={(e) => handleBorderChange(trip.tripId, 'country', e.target.value)}
              />
            </div>
            <button className="btn btn-sm mt-8" onClick={() => submitBorderCrossing(trip.tripId)}>
              <Flag size={12} /> {t('driver.submitBorderBtn')}
            </button>
          </div>
        </div>
      ))}

      {waybillCargoId && (
        <WaybillView apiUrl={`/api/driver/cargo/${waybillCargoId}/waybill`} onClose={() => setWaybillCargoId(null)} />
      )}
    </div>
  );
}
