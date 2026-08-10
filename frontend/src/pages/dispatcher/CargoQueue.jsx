import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Package, Weight, MapPin, Truck as TruckIcon, ShieldCheck, Route as RouteIcon, Fuel, Calculator, Wand2, XCircle, Users } from 'lucide-react';
import { MapContainer, TileLayer, Marker } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import axiosClient from '../../api/axiosClient';
import CapacityCheckModal from '../../components/dispatcher/CapacityCheckModal.jsx';
import CustomerInfoModal from '../../components/dispatcher/CustomerInfoModal.jsx';
import CargoCustomsPanel from '../../components/dispatcher/CargoCustomsPanel.jsx';
import RoadRoutePolyline from '../../components/RoadRoutePolyline.jsx';
import { roadDistanceKm } from '../../utils/geo.js';

const pickupIcon = new L.DivIcon({
  className: 'custom-pin',
  html: '<div style="width:24px;height:24px;border-radius:50% 50% 50% 0;background:#16a34a;transform:rotate(-45deg);border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);"></div>',
  iconSize: [24, 24],
  iconAnchor: [12, 24],
});
const destIcon = new L.DivIcon({
  className: 'custom-pin',
  html: '<div style="width:24px;height:24px;border-radius:50% 50% 50% 0;background:#dc2626;transform:rotate(-45deg);border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);"></div>',
  iconSize: [24, 24],
  iconAnchor: [12, 24],
});

// Seçilmiş gözləyən yükün götürülmə -> təhvil marşrutunu göstərən kompakt
// xəritə (bax DriverCurrentTrip.jsx-dəki DriverTripMap — eyni məntiq: hər
// iki nöqtə arasındakı məsafəyə görə avtomatik zoom hesablanır).
function CargoRouteMap({ cargo }) {
  const { t } = useTranslation();
  // Real road-route distance from OSRM (see RoadRoutePolyline), once it
  // resolves — upgrades the haversine-based estimate below with the actual
  // km the truck will drive. Reset whenever the selected cargo changes so
  // we don't show the previous cargo's distance while the new route loads.
  const [actualKm, setActualKm] = useState(null);
  useEffect(() => setActualKm(null), [cargo?.id]);

  const hasPickup = cargo?.pickupLatitude != null && cargo?.pickupLongitude != null;
  const hasDest = cargo?.destinationLatitude != null && cargo?.destinationLongitude != null;

  if (!cargo) {
    return (
      <div className="flex items-center justify-center text-muted" style={{ height: 260, fontSize: 13.5 }}>
        {t('dispatcher.selectCargoHint')}
      </div>
    );
  }

  if (!hasPickup && !hasDest) {
    return (
      <div className="flex items-center justify-center text-muted" style={{ height: 260, fontSize: 13.5 }}>
        {t('dispatcher.noCoordinates')}
      </div>
    );
  }

  const points = [];
  if (hasPickup) points.push([cargo.pickupLatitude, cargo.pickupLongitude]);
  if (hasDest) points.push([cargo.destinationLatitude, cargo.destinationLongitude]);
  const center = hasPickup && hasDest
    ? [(cargo.pickupLatitude + cargo.destinationLatitude) / 2, (cargo.pickupLongitude + cargo.destinationLongitude) / 2]
    : points[0];
  // Xam haversine (hava yolu) yox, quru yol məsafəsinə yaxın təxmin —
  // backend-in RouteEstimationService-də istifadə etdiyi eyni 1.3x
  // əmsalı ilə (bax utils/geo.js#roadDistanceKm), ki, göstərilən km
  // real yol məsafəsindən çox aşağı olmasın.
  const distanceKm = hasPickup && hasDest
    ? roadDistanceKm(cargo.pickupLatitude, cargo.pickupLongitude, cargo.destinationLatitude, cargo.destinationLongitude)
    : null;
  // Real OSRM route distance once it's loaded, otherwise the haversine-based
  // estimate above — so the label never has to wait/blank out.
  const displayDistanceKm = actualKm ?? distanceKm;
  const zoom = distanceKm == null ? 12 : distanceKm > 1500 ? 4 : distanceKm > 500 ? 5.5 : distanceKm > 250 ? 7 : distanceKm > 80 ? 8.5 : distanceKm > 20 ? 10 : 12;

  return (
    <div>
      <div style={{ height: 260, borderRadius: 10, overflow: 'hidden', border: '1px solid #e5e7eb' }}>
        {/* react-leaflet keeps the same underlying Leaflet map instance across
            re-renders unless the component itself remounts — without a key
            tied to the cargo, switching between two cargos either leaves the
            map frozen on the first one's location or (in React 18) throws
            "Map container is already initialized" and renders nothing at
            all. The key forces a clean unmount/remount per cargo. */}
        <MapContainer key={cargo.id} center={center} zoom={zoom} style={{ height: '100%', width: '100%' }}>
          <TileLayer attribution="OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          {hasPickup && <Marker position={[cargo.pickupLatitude, cargo.pickupLongitude]} icon={pickupIcon} />}
          {hasDest && <Marker position={[cargo.destinationLatitude, cargo.destinationLongitude]} icon={destIcon} />}
          {hasPickup && hasDest && (
            <RoadRoutePolyline
              points={points}
              pathOptions={{ color: 'var(--primary)', weight: 3, dashArray: '6 6' }}
              onRoute={(route) => setActualKm(route ? route.distanceKm : null)}
            />
          )}
        </MapContainer>
      </div>
      <div className="mt-8" style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 12.5 }}>
        <span className="flex items-center gap-1.5">🟢 <strong>{cargo.pickupAddress || t('dispatcher.noPickupAddress')}</strong></span>
        <span className="flex items-center gap-1.5">🔴 <strong>{cargo.destinationAddress || t('dispatcher.noDestAddress')}</strong></span>
        {displayDistanceKm != null && (
          <span className="text-muted">
            {actualKm != null ? t('dispatcher.roadDistanceNav') : t('dispatcher.roadDistanceEst')}: {displayDistanceKm.toFixed(0)} km
          </span>
        )}
      </div>
    </div>
  );
}

// Cargo -> CustomerInfoModal-ın gözlədiyi normallaşdırılmış şəkil. Qeydiyyatlı
// müştərilər üçün Cargo.customer (tam Customer obyekti, backend-dən artıq
// gəlir) istifadə olunur; dispetçerin əl ilə daxil etdiyi sifarişlərdə (bax
// NewCargo.jsx) isə yalnız Cargo.customerName/customerPhone mətn kimi
// mövcuddur.
function cargoToCustomer(c) {
  return {
    fullName: c.customer?.fullName || c.customerName,
    phone: c.customer?.phone || c.customerPhone,
    email: c.customer?.email,
    companyName: c.customer?.companyName,
    registered: Boolean(c.customer),
    trackingNumber: c.trackingNumber,
    pickupAddress: c.pickupAddress,
    // "Yazış" düyməsi CustomerInfoModal-da yalnız cargoId varsa görünür (bax
    // OrderChat/ChatController) — bu sahə əvvəllər unudulmuşdu, ona görə
    // dispetçerin əsas "Gözləyən yüklər" səhifəsində heç bir yazış düyməsi
    // görünmürdü.
    cargoId: c.id,
  };
}

export default function CargoQueue() {
  const { t } = useTranslation();
  const [cargos, setCargos] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [trailers, setTrailers] = useState([]);
  const [selected, setSelected] = useState([]);
  const [driverId, setDriverId] = useState('');
  const [vehicleId, setVehicleId] = useState('');
  const [trailerId, setTrailerId] = useState('');
  const [routeInfo, setRouteInfo] = useState('');
  const [estimatedDistanceKm, setEstimatedDistanceKm] = useState('');
  const [estimatedCost, setEstimatedCost] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [capacityWarningOpen, setCapacityWarningOpen] = useState(false);
  const [customerModalCargo, setCustomerModalCargo] = useState(null);
  const [customsCargo, setCustomsCargo] = useState(null);
  const [mapCargo, setMapCargo] = useState(null);
  const [optimizedRoute, setOptimizedRoute] = useState(null);
  const [optimizing, setOptimizing] = useState(false);
  const [optimizeError, setOptimizeError] = useState('');
  const [suggestions, setSuggestions] = useState(null);
  const [suggestLoading, setSuggestLoading] = useState(false);
  const customerModalCustomers = useMemo(
    () => (customerModalCargo ? [cargoToCustomer(customerModalCargo)] : null),
    [customerModalCargo]
  );

  const load = () => {
    axiosClient.get('/api/dispatcher/cargo/pending').then((res) => setCargos(res.data));
    axiosClient.get('/api/dispatcher/drivers/available').then((res) => setDrivers(res.data));
    axiosClient.get('/api/dispatcher/vehicles').then((res) => setVehicles(res.data));
    axiosClient.get('/api/dispatcher/trailers').then((res) => setTrailers(res.data));
  };

  useEffect(load, []);

  // Sürücü seçiləndə tır/qoşqu siyahısı o sürücüyə görə filtrlənir: şirkət
  // avadanlığı həmişə görünür, sürücüyə məxsus (DRIVER_OWNED) avadanlıq isə
  // yalnız öz sahibi seçiləndə görünür (bax backend DispatcherController#
  // allVehicles/allTrailers driverId parametri). Əvvəlki seçim yeni siyahıda
  // yoxdursa sıfırlanır ki, dispetçer səhvən başqa sürücüyə məxsus tırla
  // reys göndərməsin.
  useEffect(() => {
    const params = driverId ? `?driverId=${driverId}` : '';
    axiosClient.get(`/api/dispatcher/vehicles${params}`).then((res) => {
      setVehicles(res.data);
      setVehicleId((prev) => (prev && res.data.some((v) => String(v.id) === String(prev)) ? prev : ''));
    });
    axiosClient.get(`/api/dispatcher/trailers${params}`).then((res) => {
      setTrailers(res.data);
      setTrailerId((prev) => (prev && res.data.some((t2) => String(t2.id) === String(prev)) ? prev : ''));
    });
  }, [driverId]);

  const toggleSelect = (id) => {
    setSelected((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
    setOptimizedRoute(null);
  };

  // Xoşuna gəlməyən/qəbul etmək istəmədiyi yükü imtina edir — bax
  // DispatcherController#rejectCargo. Yük real silinmir, CANCELLED
  // statusuna keçir və (qeydiyyatlıdırsa) müştəriyə bildiriş gedir.
  const rejectCargo = async (c) => {
    if (!window.confirm(t('dispatcher.errRejectPrompt', { tracking: c.trackingNumber }))) return;
    const reason = window.prompt(t('dispatcher.rejectReasonPrompt'), '') || '';
    try {
      await axiosClient.post(`/api/dispatcher/cargo/${c.id}/reject`, { reason });
      setSelected((prev) => prev.filter((x) => x !== c.id));
      setSuccess(t('dispatcher.rejectSuccess', { tracking: c.trackingNumber }));
      load();
    } catch (err) {
      setError(err.response?.data?.message || t('dispatcher.rejectError'));
    }
  };

  const runOptimize = async () => {
    setOptimizing(true);
    setOptimizeError('');
    try {
      const res = await axiosClient.post('/api/dispatcher/route/optimize', { cargoIds: selected });
      setOptimizedRoute(res.data);
    } catch (err) {
      setOptimizeError(err.response?.data?.message || t('dispatcher.optimizeError'));
    } finally {
      setOptimizing(false);
    }
  };

  const applyOptimizedRoute = () => {
    if (!optimizedRoute) return;
    setRouteInfo(optimizedRoute.routeSummary);
    setEstimatedDistanceKm(String(optimizedRoute.totalDistanceKm));
  };

  // "Ən uyğun sürücü" təklifi (bax DriverSuggestionService) — HOS+DVIR+
  // məsafə+tutum+reytinqə görə sıralanmış siyahı gətirir, dispetçer
  // siyahıdan birbaşa seçə bilər. Sırf köməkdir, heç nəyi məcburi etmir.
  const fetchSuggestions = async () => {
    if (selected.length === 0) return;
    setSuggestLoading(true);
    try {
      const params = new URLSearchParams();
      selected.forEach((id) => params.append('cargoIds', id));
      const res = await axiosClient.get(`/api/dispatcher/drivers/suggest?${params.toString()}`);
      setSuggestions(res.data);
    } catch {
      setSuggestions([]);
    } finally {
      setSuggestLoading(false);
    }
  };

  const pickSuggestedDriver = (id) => {
    setDriverId(String(id));
    setSuggestions(null);
  };

  const selectedCargo = useMemo(() => cargos.filter((c) => selected.includes(c.id)), [cargos, selected]);
  const totalWeightKg = useMemo(() => selectedCargo.reduce((sum, c) => sum + (c.weight || 0), 0), [selectedCargo]);
  const selectedTrailer = useMemo(() => trailers.find((tr) => String(tr.id) === String(trailerId)), [trailers, trailerId]);
  const selectedDriver = useMemo(() => drivers.find((d) => String(d.id) === String(driverId)), [drivers, driverId]);

  // Tır (dartıcı) yalnız "baş hissə"dir — özü heç vaxt yük daşımır, kəllə
  // qoşquya qoşulur və yükü QOŞQU daşıyır. Ona görə tırın öz "capacity"
  // sahəsi çəki limiti kimi İSTİFADƏ OLUNMUR (bu, ötən versiyada səhv idi) —
  // yalnız qoşqunun tutumu həqiqi yük limitidir. Qoşqu seçilməyibsə limit
  // yoxdur, çünki bu halda YÜKÜ DAŞIYACAQ heç nə seçilməyib — handleCreateTrip
  // aşağıda çəkisi > 0 olan yükdə qoşqu seçimini MƏCBURİ edir.
  const effectiveCapacityTons = selectedTrailer?.capacity;

  // Real yük daşıma qiymətləndirməsinin sadələşdirilmiş modeli (bax backend
  // TripCostEstimationService: yanacaq + sürücü əməkhaqqı + texniki xidmət +
  // baza xərc, üzərinə yük növü/təcililik əlavələri) — seçim və ya tır
  // dəyişən kimi "sistem təklifi" kimi gətirilir və xanalara avtomatik
  // yazılır. Dispetçer dəyəri əl ilə düzəltsə, növbəti avtomatik yeniləmə
  // onun üzərinə yazmır (yalnız hələ toxunulmamış/əvvəlki təklifə bərabər
  // qalan xanalar yenilənir).
  const [costEstimate, setCostEstimate] = useState(null);
  const [estimateLoading, setEstimateLoading] = useState(false);
  const lastSuggested = useRef({ distance: null, cost: null });
  const selectedKey = selected.slice().sort((a, b) => a - b).join(',');

  // Yük seçimi dəyişəndə köhnə təklif siyahısı artıq yararsızdır (başqa
  // götürülmə nöqtəsi/çəki üçün hesablanıb) — gizlədilir, dispetçer yenidən
  // "Ən uyğun sürücünü tap" düyməsinə basmalıdır.
  useEffect(() => { setSuggestions(null); }, [selectedKey]);

  useEffect(() => {
    if (selected.length === 0 || !vehicleId) {
      setCostEstimate(null);
      return undefined;
    }
    let cancelled = false;
    setEstimateLoading(true);
    const params = new URLSearchParams();
    selected.forEach((id) => params.append('cargoIds', id));
    params.append('vehicleId', vehicleId);
    // Ağır yükdə yanacaq əlavəsi indi QOŞQUNUN tutumuna görə hesablanır (bax
    // TripCostEstimationService) — tırın özündə artıq capacity sahəsi yoxdur.
    if (trailerId) params.append('trailerId', trailerId);
    axiosClient.get(`/api/dispatcher/trips/cost-estimate?${params.toString()}`)
      .then((res) => {
        if (cancelled) return;
        setCostEstimate(res.data);
        setEstimatedDistanceKm((prev) => (
          prev === '' || prev === String(lastSuggested.current.distance) ? String(res.data.distanceKm) : prev
        ));
        setEstimatedCost((prev) => (
          prev === '' || prev === String(lastSuggested.current.cost) ? String(res.data.totalCost) : prev
        ));
        lastSuggested.current = { distance: res.data.distanceKm, cost: res.data.totalCost };
      })
      .catch(() => { if (!cancelled) setCostEstimate(null); })
      .finally(() => { if (!cancelled) setEstimateLoading(false); });
    return () => { cancelled = true; };
  }, [selectedKey, vehicleId, trailerId]);

  const submitTrip = async () => {
    try {
      await axiosClient.post('/api/dispatcher/trips', {
        driverId: parseInt(driverId),
        vehicleId: parseInt(vehicleId),
        trailerId: trailerId ? parseInt(trailerId) : null,
        cargoIds: selected,
        routeInfo,
        estimatedDistanceKm: estimatedDistanceKm ? parseFloat(estimatedDistanceKm) : null,
        estimatedCost: estimatedCost ? parseFloat(estimatedCost) : null,
      });
      setSuccess(t('dispatcher.tripCreated'));
      setSelected([]);
      setDriverId('');
      setVehicleId('');
      setTrailerId('');
      setRouteInfo('');
      setEstimatedDistanceKm('');
      setEstimatedCost('');
      lastSuggested.current = { distance: null, cost: null };
      setCostEstimate(null);
      load();
    } catch (err) {
      setError(err.response?.data?.message || t('dispatcher.tripCreateError'));
    }
  };

  const handleCreateTrip = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (selected.length === 0) {
      setError(t('dispatcher.errSelectCargo'));
      return;
    }
    if (!driverId || !vehicleId) {
      setError(t('dispatcher.errSelectDriverVehicle'));
      return;
    }
    // Tır özü yük daşımır (yalnız kəllə hissəsidir) — çəkisi olan yük üçün
    // qoşqu seçimi məcburidir, əks halda yükü daşıyacaq heç nə yoxdur.
    if (totalWeightKg > 0 && !trailerId) {
      setError(t('dispatcher.errTrailerRequired'));
      return;
    }

    // Stage 4 enterprise validation: block (with an override option) if the
    // selected cargo's combined weight exceeds the rated capacity (in tons,
    // Cargo.weight in kg). effectiveCapacityTons is always the TRAILER's
    // rating — the truck head itself carries no cargo, so its own capacity
    // is never used as a weight limit (see the field's declaration above).
    if (effectiveCapacityTons != null) {
      const capacityKg = effectiveCapacityTons * 1000;
      if (totalWeightKg > capacityKg) {
        setCapacityWarningOpen(true);
        return;
      }
    }

    await submitTrip();
  };

  return (
    <div>
      <h2>{t('dispatcher.queueTitle')}</h2>
      <p>{t('dispatcher.queueDesc')}</p>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="table-wrap mt-16">
        <table>
          <thead>
            <tr>
              <th></th>
              <th>{t('dispatcher.colTracking')}</th>
              <th>{t('dispatcher.colCustomer')}</th>
              <th>{t('dispatcher.colDescription')}</th>
              <th>{t('dispatcher.colWeight')}</th>
              <th>{t('dispatcher.colDestination')}</th>
              <th>{t('dispatcher.colType')}</th>
              <th>{t('dispatcher.colUrgency')}</th>
              <th>{t('dispatcher.colCustoms')}</th>
              <th>{t('dispatcher.colMap')}</th>
              <th>{t('dispatcher.colAction')}</th>
            </tr>
          </thead>
          <tbody>
            {cargos.map((c) => (
              <tr key={c.id} style={mapCargo?.id === c.id ? { background: '#f7f8fb' } : undefined}>
                <td><input type="checkbox" checked={selected.includes(c.id)} onChange={() => toggleSelect(c.id)} /></td>
                <td>{c.trackingNumber}</td>
                <td>
                  <button
                    type="button"
                    onClick={() => setCustomerModalCargo(c)}
                    className="btn btn-sm"
                    style={{ borderColor: 'transparent', background: 'transparent', color: 'var(--primary)', padding: '4px 6px' }}
                  >
                    {c.customer?.fullName || c.customerName || t('dispatcher.unknownCustomer')}
                  </button>
                </td>
                <td>{c.description}</td>
                <td>{c.weight} kg</td>
                <td>{c.destinationAddress}</td>
                <td>{c.cargoType}</td>
                <td>{c.urgency}</td>
                <td>
                  {c.requiresCustoms ? (
                    <button
                      type="button"
                      onClick={() => setCustomsCargo(c)}
                      className="btn btn-sm"
                      style={{ gap: 5 }}
                    >
                      <ShieldCheck size={12} /> {t('dispatcher.internationalBtn')}
                    </button>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </td>
                <td>
                  <button
                    type="button"
                    onClick={() => setMapCargo(c)}
                    className="btn btn-sm"
                    style={{ gap: 5, background: mapCargo?.id === c.id ? 'var(--primary)' : undefined, borderColor: mapCargo?.id === c.id ? 'var(--primary)' : undefined, color: mapCargo?.id === c.id ? '#fff' : undefined }}
                  >
                    <MapPin size={12} /> {t('dispatcher.showBtn')}
                  </button>
                </td>
                <td>
                  <button
                    type="button"
                    onClick={() => rejectCargo(c)}
                    className="btn btn-sm btn-danger"
                    style={{ gap: 5 }}
                    title={t('dispatcher.rejectTitle')}
                  >
                    <XCircle size={12} /> {t('dispatcher.rejectBtn')}
                  </button>
                </td>
              </tr>
            ))}
            {cargos.length === 0 && (
              <tr><td colSpan={11} className="text-center text-muted">{t('dispatcher.noPendingCargo')}</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="card mt-24">
        <h3 className="flex items-center gap-1.5">
          <RouteIcon size={16} style={{ color: 'var(--primary)' }} />
          {mapCargo ? t('dispatcher.routeMapTitleWithTracking', { tracking: mapCargo.trackingNumber }) : t('dispatcher.routeMapTitle')}
        </h3>
        <CargoRouteMap cargo={mapCargo} />
      </div>

      <div className="card mt-24" style={{ maxWidth: 520 }}>
        <h3>{t('dispatcher.createTripTitle', { count: selected.length })}</h3>

        {selected.length > 0 && (
          <div className="flex mb-16" style={{ flexWrap: 'wrap', gap: 16, border: '1px solid #e5e7eb', borderRadius: 10, padding: 14, fontSize: 13 }}>
            <div className="flex items-center gap-1.5">
              <Package size={14} style={{ color: 'var(--primary)' }} /> {t('dispatcher.cargoCount', { count: selected.length })}
            </div>
            <div className="flex items-center gap-1.5">
              <Weight size={14} style={{ color: 'var(--primary)' }} /> {totalWeightKg.toLocaleString()} kg
            </div>
            {selectedTrailer?.capacity != null && (
              <div className="flex items-center gap-1.5 text-muted">
                <TruckIcon size={14} /> {t('dispatcher.trailerCapacityLabel', { capacity: selectedTrailer.capacity })}
              </div>
            )}
            {effectiveCapacityTons != null && (
              <div
                className="flex items-center gap-1.5"
                style={{
                  fontWeight: totalWeightKg > effectiveCapacityTons * 1000 ? 600 : 400,
                  color: totalWeightKg > effectiveCapacityTons * 1000 ? 'var(--danger)' : 'var(--success)',
                }}
              >
                <Weight size={14} /> {t('dispatcher.capacityLabel', { capacity: effectiveCapacityTons })}
              </div>
            )}
          </div>
        )}

        {selected.length >= 2 && (
          <div className="mb-16" style={{ border: '1px dashed #e5e7eb', borderRadius: 10, padding: 14 }}>
            <div className="flex-between">
              <div className="flex items-center gap-1.5" style={{ fontWeight: 600, fontSize: 13.5 }}>
                <Wand2 size={14} style={{ color: 'var(--primary)' }} /> {t('dispatcher.multiStopTitle')}
              </div>
              <button type="button" className="btn btn-sm" onClick={runOptimize} disabled={optimizing}>
                {optimizing ? t('dispatcher.calculating') : t('dispatcher.findShortestBtn')}
              </button>
            </div>
            {optimizeError && <p style={{ color: 'var(--danger)', fontSize: 12, marginTop: 8 }}>{optimizeError}</p>}
            {optimizedRoute && (
              <div className="mt-8" style={{ fontSize: 12.5 }}>
                <div style={{ marginBottom: 6, color: '#374151' }}>
                  {optimizedRoute.start?.label}
                  {optimizedRoute.orderedStops.map((s, i) => (
                    <span key={s.cargoId || i}> → <strong>{s.label}</strong></span>
                  ))}
                </div>
                <div className="flex items-center gap-1.5" style={{ color: 'var(--text-muted)' }}>
                  <span>{t('dispatcher.optimizedDistance')}: <strong>{optimizedRoute.totalDistanceKm} km</strong></span>
                  {optimizedRoute.naiveDistanceKm > optimizedRoute.totalDistanceKm && (
                    <span style={{ color: 'var(--success)' }}>
                      {t('dispatcher.shorterBy', { km: (optimizedRoute.naiveDistanceKm - optimizedRoute.totalDistanceKm).toFixed(1) })}
                    </span>
                  )}
                </div>
                <button type="button" className="btn btn-sm btn-primary mt-8" onClick={applyOptimizedRoute}>
                  {t('dispatcher.applyRouteBtn')}
                </button>
              </div>
            )}
          </div>
        )}

        <form onSubmit={handleCreateTrip}>
          {/* "Ən uyğun sürücü" təklifi — real TMS platformalarında (Motive/
              Samsara) dispetçer sürücünü əl ilə axtarmaq əvəzinə sistemin
              HOS+DVIR+məsafə+tutum+reytinqə görə sıraladığı siyahıdan seçir
              (bax DriverSuggestionService). Sırf köməkdir, aşağıdakı adi
              seçim siyahısı da hər zaman açıqdır. */}
          {selected.length > 0 && (
            <div className="mb-16" style={{ border: '1px dashed #e5e7eb', borderRadius: 10, padding: 14 }}>
              <div className="flex-between">
                <div className="flex items-center gap-1.5" style={{ fontWeight: 600, fontSize: 13.5 }}>
                  <Users size={14} style={{ color: 'var(--primary)' }} /> {t('dispatcher.suggestDriversTitle')}
                </div>
                <button type="button" className="btn btn-sm" onClick={fetchSuggestions} disabled={suggestLoading}>
                  {suggestLoading ? t('dispatcher.calculating') : t('dispatcher.suggestDriversBtn')}
                </button>
              </div>
              {suggestions && suggestions.length === 0 && (
                <p className="text-muted mt-8" style={{ fontSize: 12.5 }}>{t('dispatcher.suggestDriversEmpty')}</p>
              )}
              {suggestions && suggestions.length > 0 && (
                <div className="mt-8" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {suggestions.slice(0, 5).map((s, i) => (
                    <div
                      key={s.id}
                      className="flex-between"
                      style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: '6px 10px', fontSize: 12.5, gap: 8 }}
                    >
                      <div>
                        <div style={{ fontWeight: 600 }}>#{i + 1} {s.fullName}</div>
                        <div className="text-muted" style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 2 }}>
                          <span>{t('dispatcher.suggestScoreLabel', { score: s.score })}</span>
                          <span>
                            {s.distanceKm != null
                              ? t('dispatcher.suggestDistanceLabel', { km: s.distanceKm })
                              : t('dispatcher.suggestDistanceUnknown')}
                          </span>
                          {s.hasUnresolvedDvirDefect && <span style={{ color: 'var(--danger)' }}>{t('dispatcher.driverDvirWarnShort')}</span>}
                          {s.fatigueWarning && <span style={{ color: 'var(--danger)' }}>{t('dispatcher.driverFatigueWarnShort')}</span>}
                          {s.capacitySufficient === false && (
                            <span style={{ color: 'var(--danger)' }}>{t('dispatcher.suggestCapacityInsufficient')}</span>
                          )}
                        </div>
                      </div>
                      <button type="button" className="btn btn-sm btn-primary" onClick={() => pickSuggestedDriver(s.id)}>
                        {t('dispatcher.suggestPickBtn')}
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          <div className="form-group">
            <label className="label">{t('dispatcher.driverLabel')}</label>
            <select className="input" value={driverId} onChange={(e) => setDriverId(e.target.value)} required>
              <option value="">{t('dispatcher.selectPlaceholder')}</option>
              {drivers.map((d) => {
                const flags = [
                  d.hasUnresolvedDvirDefect ? t('dispatcher.driverDvirWarnShort') : null,
                  d.fatigueWarning ? t('dispatcher.driverFatigueWarnShort') : null,
                ].filter(Boolean).join(' · ');
                return (
                  <option key={d.id} value={d.id}>
                    {d.fullName} — {d.phone}{flags ? ` · ⚠ ${flags}` : ''}
                  </option>
                );
              })}
            </select>
            {/* Real TMS platformalarında (Motive/Samsara) dispetçer sürücünü
                TƏYİN ETMƏZDƏN ƏVVƏL onun HOS (iş saatı) vəziyyətini və həll
                olunmamış DVIR defektini görür — köhnə versiyada yalnız ad/
                telefon göstərilirdi, uyğun olmayan sürücü seçimi heç bir
                xəbərdarlıq vermirdi (bax DriverAvailabilityResponse). Sırf
                görünürlük — backend enforcement yoxdur, dispetçer yenə də
                əl ilə davam edə bilər (CapacityCheckModal-dakı kimi). */}
            {selectedDriver && (
              <div
                className="mt-8"
                style={{
                  border: '1px solid #e5e7eb',
                  borderRadius: 10,
                  padding: '8px 12px',
                  fontSize: 12.5,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 4,
                }}
              >
                {selectedDriver.hasUnresolvedDvirDefect && (
                  <div style={{ color: 'var(--danger)', fontWeight: 600 }}>{t('dispatcher.driverDvirWarnFull')}</div>
                )}
                {selectedDriver.fatigueWarning && (
                  <div style={{ color: 'var(--danger)', fontWeight: 600 }}>{t('dispatcher.driverFatigueWarnFull')}</div>
                )}
                <div className="text-muted">
                  {selectedDriver.hasActiveTrip
                    ? t('dispatcher.driverHosRemaining', { hours: selectedDriver.remainingDrivingHours })
                    : t('dispatcher.driverHosAvailable')}
                </div>
                <div className="text-muted">
                  {selectedDriver.ratingCount > 0
                    ? t('dispatcher.driverRatingLabel', { avg: selectedDriver.ratingAverage, count: selectedDriver.ratingCount })
                    : t('dispatcher.driverNoRating')}
                </div>
              </div>
            )}
          </div>
          <div className="form-group">
            <label className="label">{t('dispatcher.vehicleLabel')}</label>
            <select className="input" value={vehicleId} onChange={(e) => setVehicleId(e.target.value)} required>
              <option value="">{t('dispatcher.selectPlaceholder')}</option>
              {vehicles.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.plateNumber} — {v.brand}{v.ownerType === 'DRIVER_OWNED' ? ` · ${t('dispatcher.ownerDriverSuffix')}` : ` · ${t('dispatcher.ownerCompanySuffix')}`}
                </option>
              ))}
            </select>
            {!driverId && (
              <span className="text-muted" style={{ fontSize: 11.5, display: 'block', marginTop: 4 }}>
                {t('dispatcher.vehicleFilterHint')}
              </span>
            )}
          </div>
          <div className="form-group">
            <label className="label">{t('dispatcher.trailerLabel')}</label>
            <select className="input" value={trailerId} onChange={(e) => setTrailerId(e.target.value)} required={totalWeightKg > 0}>
              <option value="">{t('dispatcher.trailerNone')}</option>
              {trailers.map((tr) => (
                <option key={tr.id} value={tr.id}>
                  {tr.plateNumber} ({tr.capacity} ton){tr.ownerType === 'DRIVER_OWNED' ? ` · ${t('dispatcher.ownerDriverSuffix')}` : ` · ${t('dispatcher.ownerCompanySuffix')}`}
                </option>
              ))}
            </select>
            {totalWeightKg > 0 && !trailerId && (
              <span className="text-muted" style={{ fontSize: 11.5, display: 'block', marginTop: 4, color: 'var(--danger)' }}>
                {t('dispatcher.trailerRequiredHint')}
              </span>
            )}
          </div>
          <div className="form-group">
            <label className="label">{t('dispatcher.routeInfoLabel')}</label>
            <input className="input" value={routeInfo} onChange={(e) => setRouteInfo(e.target.value)} placeholder={t('dispatcher.routeInfoPlaceholder')} />
          </div>
          <div className="form-group">
            <label className="label">{t('dispatcher.estDistanceLabel')}</label>
            <input className="input" value={estimatedDistanceKm} onChange={(e) => setEstimatedDistanceKm(e.target.value)} />
          </div>
          <div className="form-group">
            <label className="label">{t('dispatcher.priceLabel')}</label>
            <input className="input" value={estimatedCost} onChange={(e) => setEstimatedCost(e.target.value)} />
            {estimateLoading && <span className="text-muted" style={{ fontSize: 12 }}>{t('dispatcher.suggestionCalculating')}</span>}
            <span className="text-muted" style={{ fontSize: 11.5, display: 'block', marginTop: 4 }}>
              {t('dispatcher.priceHint')}
            </span>
          </div>

          {/* Sistem təklifinin tərkib hissələri — real yük daşıma qiymətinin
              necə qurulduğunu göstərir (yanacaq + sürücü + servis + baza xərc,
              üzərinə yük növü/təcililik əlavələri). Sırf məlumat üçündür,
              yuxarıdakı xanalar hələ də əl ilə redaktə oluna bilər. */}
          {costEstimate && !estimateLoading && (
            <div
              className="mb-16"
              style={{ border: '1px dashed #e5e7eb', borderRadius: 10, padding: '10px 14px', fontSize: 12.5, background: '#f9fafb' }}
            >
              <div className="flex items-center gap-1.5" style={{ fontWeight: 600, marginBottom: 6, color: '#374151' }}>
                <Calculator size={13} style={{ color: 'var(--primary)' }} /> {t('dispatcher.suggestionTitle', { km: costEstimate.distanceKm })}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3, color: 'var(--text-muted)' }}>
                <span className="flex items-center gap-1.5"><Fuel size={11} /> {t('dispatcher.fuelCost')}: {costEstimate.fuelCost} ₼</span>
                <span>{t('dispatcher.driverCost')}: {costEstimate.driverCost} ₼</span>
                <span>{t('dispatcher.maintenanceCost')}: {costEstimate.maintenanceCost} ₼</span>
                <span>{t('dispatcher.baseFee')}: {costEstimate.baseFee} ₼</span>
                {costEstimate.handlingSurchargePercent > 0 && (
                  <span>{t('dispatcher.handlingSurcharge')}: +{costEstimate.handlingSurchargePercent}%</span>
                )}
                {costEstimate.urgencySurchargePercent > 0 && (
                  <span>{t('dispatcher.urgencySurcharge')}: +{costEstimate.urgencySurchargePercent}%</span>
                )}
              </div>
            </div>
          )}

          <div className="flex items-center gap-1.5 mb-16 text-xs text-muted">
            <MapPin size={12} /> {t('dispatcher.willAppearHint')}
          </div>
          <button className="btn btn-primary btn-block" type="submit">{t('dispatcher.createTripBtn')}</button>
        </form>
      </div>

      <CapacityCheckModal
        open={capacityWarningOpen}
        totalWeightKg={totalWeightKg}
        capacityTons={effectiveCapacityTons}
        onCancel={() => setCapacityWarningOpen(false)}
        onConfirm={async () => {
          setCapacityWarningOpen(false);
          await submitTrip();
        }}
      />

      <CustomerInfoModal customers={customerModalCustomers} onClose={() => setCustomerModalCargo(null)} />
      {customsCargo && <CargoCustomsPanel cargo={customsCargo} onClose={() => setCustomsCargo(null)} />}
    </div>
  );
}
