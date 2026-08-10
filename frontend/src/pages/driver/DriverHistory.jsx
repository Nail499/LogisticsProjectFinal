import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Route as RouteIcon, MapPin, Flag, Calendar, Gauge, Wallet, Search, FileText } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import TripDetailModal from '../../components/TripDetailModal.jsx';
import WaybillView from '../../components/WaybillView.jsx';

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('az-AZ', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ', ' + d.toLocaleTimeString('az-AZ', { hour: '2-digit', minute: '2-digit' });
}

// Əvvəllər sadə cədvəl idi və /api/driver/trips/history bare Trip
// qaytardığı üçün (Trip.cargos @JsonIgnore) heç bir ünvan/tracking № göstərə
// bilmirdi. İndi backend zənginləşdirilmiş LiveTripResponse qaytarır (bax
// DriverController#tripHistory) və hər reys kartı üçün "Ətraflı bax" düyməsi
// var — TripDetailModal-ı açır, o da mövcud LiveTrackingPanel-i (xəritədə
// real yol marşrutu, qət edilmiş/qalan məsafə, mərhələ zolağı, yol boyu
// xərclər və s.) yenidən istifadə edir.
export default function DriverHistory() {
  const { t } = useTranslation();
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [detailTrackingNumber, setDetailTrackingNumber] = useState(null);
  const [waybillCargoId, setWaybillCargoId] = useState(null);

  useEffect(() => {
    axiosClient.get('/api/driver/trips/history')
      .then((res) => setTrips(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <h2>{t('driver.historyTitle')}</h2>
      <p>{t('driver.historyDesc')}</p>

      {trips.length === 0 && (
        <div className="card mt-16 text-center text-muted">{t('driver.historyEmpty')}</div>
      )}

      {trips.map((trip) => (
        <div className="card hover-lift mt-16" key={trip.tripId}>
          <div className="flex-between" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 10 }}>
            <div style={{ minWidth: 0 }}>
              <div className="flex items-center gap-1.5 text-xs text-muted">
                <RouteIcon size={12} /> {t('driver.tripLabel')}{trip.tripId}
              </div>
              <h3 style={{ margin: '4px 0 0' }}>{trip.routeInfo || '—'}</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3, marginTop: 6, fontSize: 13 }}>
                {trip.pickupAddress && (
                  <span className="flex items-center gap-1.5 text-muted">
                    🟢 {trip.pickupAddress}
                  </span>
                )}
                {trip.destinationAddress && (
                  <span className="flex items-center gap-1.5 text-muted">
                    <MapPin size={13} style={{ color: 'var(--primary)', flexShrink: 0 }} /> {trip.destinationAddress}
                  </span>
                )}
              </div>
            </div>
            <span className="badge badge-success" style={{ flexShrink: 0 }}>{t('driver.deliveredBadge')}</span>
          </div>

          <div className="flex mt-16" style={{ flexWrap: 'wrap', gap: 16, border: '1px solid #e5e7eb', borderRadius: 10, padding: 12, fontSize: 13 }}>
            <span className="flex items-center gap-1.5"><Calendar size={13} style={{ color: 'var(--primary)' }} /> {t('driver.pickedUpAt')}: {formatDate(trip.startedAt)}</span>
            <span className="flex items-center gap-1.5"><Flag size={13} style={{ color: 'var(--success)' }} /> {t('driver.deliveredAt')}: {formatDate(trip.deliveredAt)}</span>
            {trip.estimatedDistanceKm != null && (
              <span className="flex items-center gap-1.5"><Gauge size={13} style={{ color: 'var(--primary)' }} /> {trip.estimatedDistanceKm} km</span>
            )}
            {trip.estimatedCost != null && (
              <span className="flex items-center gap-1.5"><Wallet size={13} style={{ color: 'var(--primary)' }} /> {trip.estimatedCost} ₼</span>
            )}
          </div>

          {trip.customers && trip.customers.length > 0 && (
            <div className="mt-16" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {trip.customers.map((c, i) => (
                <div key={c.trackingNumber || i} className="flex items-center gap-1.5">
                  <button
                    type="button"
                    className="btn btn-sm"
                    onClick={() => setDetailTrackingNumber(c.trackingNumber)}
                    disabled={!c.trackingNumber}
                    style={{ gap: 6 }}
                  >
                    <Search size={13} /> {t('driver.detailBtn')}
                    {trip.customers.length > 1 ? ` — ${c.fullName || c.trackingNumber}` : ''}
                  </button>
                  {c.cargoId && (
                    <button
                      type="button"
                      className="btn btn-sm"
                      onClick={() => setWaybillCargoId(c.cargoId)}
                      style={{ gap: 6 }}
                    >
                      <FileText size={13} /> {t('driver.waybillBtn')}
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}

      <TripDetailModal trackingNumber={detailTrackingNumber} onClose={() => setDetailTrackingNumber(null)} />
      {waybillCargoId && (
        <WaybillView apiUrl={`/api/driver/cargo/${waybillCargoId}/waybill`} onClose={() => setWaybillCargoId(null)} />
      )}
    </div>
  );
}
