// Stage 4 — Control Tower: global map of every active truck. Clicking a
// truck computes the 3 closest warehouses client-side (haversine, see
// utils/geo.js) so no dedicated backend geospatial query is needed — the
// dispatcher already has the full warehouse list via /api/dispatcher/warehouses.
// Restyled to the site's light Fleetra theme (was the old dark command-
// center look); also now shows which customer(s) the selected truck's
// trip belongs to, clickable for full contact details.
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MapContainer, TileLayer, Marker, Popup, useMap, ZoomControl } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Warehouse, Truck, X, Users } from 'lucide-react';
import { closestByDistance } from '../../utils/geo';
import CustomerInfoModal from './CustomerInfoModal.jsx';
import RoadRoutePolyline from '../RoadRoutePolyline.jsx';
import TripSearchBox from './TripSearchBox.jsx';

// Axtarışdan/işarədən seçilmiş reysin üstünə xəritəni uçur (flyTo). Ayrıca
// komponent kimi saxlanılır çünki useMap() yalnız <MapContainer>-in ÖVLADI
// olan komponentlərdə işləyir — birbaşa ControlTowerMap-in özündə çağırıla
// bilməz (o, MapContainer-i əhatə edən valideyndir, övladı deyil).
function FlyToSelected({ trip }) {
  const map = useMap();
  useEffect(() => {
    if (trip?.lastLatitude != null && trip?.lastLongitude != null) {
      map.flyTo([trip.lastLatitude, trip.lastLongitude], 13, { duration: 0.8 });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trip?.tripId]);
  return null;
}

const STATUS_COLOR = {
  PLANNED: '#94A3B8',
  PICKED_UP: '#fe8704',
  IN_TRANSIT: '#16a34a',
};
const STATUS_KEY = {
  PLANNED: 'statusPlanned',
  PICKED_UP: 'statusPickedUp',
  IN_TRANSIT: 'statusInTransit',
};

function truckDivIcon(color) {
  return new L.DivIcon({
    className: '',
    html: `<div style="font-size:22px;filter:drop-shadow(0 2px 5px rgba(0,0,0,0.35))">🚛</div>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  });
}

const warehouseIcon = new L.DivIcon({
  className: '',
  html: '<div style="width:14px;height:14px;border-radius:4px;background:#2563eb;border:2px solid white;box-shadow:0 0 0 4px rgba(37,99,235,0.2)"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

export default function ControlTowerMap({ liveTrips, warehouses }) {
  const { t } = useTranslation();
  const [selectedTrip, setSelectedTrip] = useState(null);
  const [showCustomers, setShowCustomers] = useState(false);

  const INACTIVE_MAP_STATUSES = ['DELIVERED', 'AWAITING_PAYMENT', 'PENDING_ACCEPTANCE', 'REJECTED', 'CANCELLED'];
  const activeTrips = useMemo(
    () => liveTrips.filter((tr) =>
      !INACTIVE_MAP_STATUSES.includes(tr.status) && tr.lastLatitude != null && tr.lastLongitude != null),
    [liveTrips]
  );

  // Seçilmiş reysin ƏN son versiyası — WebSocket-dən liveTrips yenilənəndə
  // (bax ControlTower.jsx) selectedTrip köhnə görüntü qalmasın deyə tripId
  // üzrə yenidən tapılır (marşrut xətti də bununla canlı yenilənir).
  const currentSelectedTrip = useMemo(() => {
    if (!selectedTrip) return null;
    return activeTrips.find((tr) => tr.tripId === selectedTrip.tripId) || selectedTrip;
  }, [selectedTrip, activeTrips]);

  const closestWarehouses = useMemo(() => {
    if (!currentSelectedTrip) return [];
    return closestByDistance(currentSelectedTrip.lastLatitude, currentSelectedTrip.lastLongitude, warehouses, 3, (w) => [w.latitude, w.longitude]);
  }, [currentSelectedTrip, warehouses]);

  const ROUTE_LINE_STATUSES = ['PICKED_UP', 'IN_TRANSIT'];
  const showRouteLine = currentSelectedTrip
    && ROUTE_LINE_STATUSES.includes(currentSelectedTrip.status)
    && currentSelectedTrip.lastLatitude != null && currentSelectedTrip.lastLongitude != null
    && currentSelectedTrip.destinationLatitude != null && currentSelectedTrip.destinationLongitude != null;

  return (
    <div className="flex flex-col gap-4 lg:flex-row">
      <div style={{ position: 'relative', height: 480, borderRadius: 14, overflow: 'hidden', border: '1px solid #e5e7eb', flex: 1 }}>
        <MapContainer center={[40.1431, 47.5769]} zoom={7} zoomControl={false} style={{ height: '100%', width: '100%' }}>
          {/* Axtarış qutusu yuxarı-sol küncü tutduğu üçün zoom düymələri
              aşağı-sağa köçürülüb ki, ikisi üst-üstə düşməsin. */}
          <ZoomControl position="bottomright" />
          <TileLayer attribution="OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          {warehouses.map((w) => (
            <Marker key={`wh-${w.id}`} position={[w.latitude, w.longitude]} icon={warehouseIcon}>
              <Popup>
                <strong>{w.name}</strong>
                <br />
                {w.address}
              </Popup>
            </Marker>
          ))}
          {activeTrips.map((tr) => (
            <Marker
              key={tr.tripId}
              position={[tr.lastLatitude, tr.lastLongitude]}
              icon={truckDivIcon(STATUS_COLOR[tr.status] || '#fe8704')}
              eventHandlers={{ click: () => setSelectedTrip(tr) }}
            />
          ))}
          {showRouteLine && (
            <RoadRoutePolyline
              points={[
                [currentSelectedTrip.lastLatitude, currentSelectedTrip.lastLongitude],
                [currentSelectedTrip.destinationLatitude, currentSelectedTrip.destinationLongitude],
              ]}
              pathOptions={{ color: STATUS_COLOR[currentSelectedTrip.status] || '#16a34a', weight: 4, opacity: 0.8, dashArray: '8 6' }}
            />
          )}
          <TripSearchBox trips={activeTrips} onSelect={setSelectedTrip} />
          <FlyToSelected trip={selectedTrip} />
        </MapContainer>
        <div style={{ position: 'absolute', right: 12, top: 12, zIndex: 1000, borderRadius: 8, background: '#fff', padding: '6px 12px', fontSize: 12, fontWeight: 600, boxShadow: '0 2px 8px rgba(0,0,0,0.12)' }}>
          {t('dispatcher.activeTrucksCount', { count: activeTrips.length })}
        </div>
      </div>

      {/* Side panel: closest-3-warehouses + customer(s) on click */}
      <div className="card" style={{ width: '100%', maxWidth: 300, flexShrink: 0 }}>
        <div className="flex items-center gap-1.5 text-xs text-muted" style={{ fontWeight: 600, textTransform: 'uppercase' }}>
          <Truck size={12} /> {t('dispatcher.selectedTruck')}
        </div>
        {!currentSelectedTrip ? (
          <p className="text-muted mt-8" style={{ fontSize: 13 }}>{t('dispatcher.selectTruckHint')}</p>
        ) : (
          <>
            <div className="flex-between mt-8">
              <div>
                <div style={{ fontWeight: 700 }}>
                  {currentSelectedTrip.vehiclePlate || `#${currentSelectedTrip.tripId}`}
                  {currentSelectedTrip.trailerPlate ? ` + ${currentSelectedTrip.trailerPlate}` : ''}
                </div>
                <div className="text-muted" style={{ fontSize: 12 }}>
                  {currentSelectedTrip.driverName || '—'}{currentSelectedTrip.driverPhone ? ` · ${currentSelectedTrip.driverPhone}` : ''}
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span className="badge badge-info">{STATUS_KEY[currentSelectedTrip.status] ? t(`driver.${STATUS_KEY[currentSelectedTrip.status]}`) : currentSelectedTrip.status}</span>
                <button type="button" onClick={() => setSelectedTrip(null)} className="btn btn-sm" style={{ padding: 5 }}>
                  <X size={13} />
                </button>
              </div>
            </div>

            {showRouteLine && (
              <p className="text-muted" style={{ margin: '6px 0 0', fontSize: 10.5 }}>{t('dispatcher.tripSearchRouteHint')}</p>
            )}

            {currentSelectedTrip.customers?.length > 0 && (
              <button
                type="button"
                onClick={() => setShowCustomers(true)}
                className="btn btn-sm mt-16"
                style={{ width: '100%', justifyContent: 'center', gap: 6 }}
              >
                <Users size={14} /> {currentSelectedTrip.customers.length > 1 ? t('dispatcher.customersPlural', { count: currentSelectedTrip.customers.length }) : t('dispatcher.customersSingle')}
              </button>
            )}

            <div className="flex items-center gap-1.5 mt-16 text-xs text-muted" style={{ fontWeight: 600, textTransform: 'uppercase', marginBottom: 6 }}>
              <Warehouse size={12} /> {t('dispatcher.closestWarehouses')}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {closestWarehouses.map((w, i) => (
                <div key={w.id} className="flex-between" style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: '8px 10px' }}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: 12.5, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {i + 1}. {w.name}
                    </div>
                    <div className="text-muted" style={{ fontSize: 11, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{w.address}</div>
                  </div>
                  <span style={{ marginLeft: 8, flexShrink: 0, fontSize: 12, fontWeight: 700, color: 'var(--primary)' }}>{w.distanceKm.toFixed(1)} km</span>
                </div>
              ))}
              {closestWarehouses.length === 0 && (
                <p className="text-muted" style={{ fontSize: 12 }}>{t('dispatcher.noWarehouses')}</p>
              )}
            </div>
          </>
        )}
      </div>

      {showCustomers && (
        <CustomerInfoModal customers={currentSelectedTrip?.customers} onClose={() => setShowCustomers(false)} />
      )}
    </div>
  );
}
