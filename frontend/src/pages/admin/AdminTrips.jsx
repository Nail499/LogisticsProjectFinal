import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Users, Flag, Search, FileDown } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import CustomerInfoModal from '../../components/dispatcher/CustomerInfoModal.jsx';
import TripBorderPanel from '../../components/dispatcher/TripBorderPanel.jsx';
import TripDetailModal from '../../components/TripDetailModal.jsx';
import { downloadCsv } from '../../utils/csvExport.js';

const STATUS_KEY = {
  AWAITING_PAYMENT: { key: 'statusAwaitingPayment', className: 'badge-warning' },
  PENDING_ACCEPTANCE: { key: 'statusPendingAcceptance', className: 'badge-warning' },
  PLANNED: { key: 'statusPlanned', className: 'badge-neutral' },
  PICKED_UP: { key: 'statusPickedUp', className: 'badge-info' },
  IN_TRANSIT: { key: 'statusInTransit', className: 'badge-info' },
  DELIVERED: { key: 'statusDelivered', className: 'badge-success' },
  REJECTED: { key: 'statusRejected', className: 'badge-danger' },
  CANCELLED: { key: 'statusCancelled', className: 'badge-danger' },
};

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('az-AZ', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ', ' + d.toLocaleTimeString('az-AZ', { hour: '2-digit', minute: '2-digit' });
}

// Admin üçün indiyə qədər aparılmış BÜTÜN reyslərin (istənilən status —
// planlaşdırılıb/yoldadır/çatdırılıb) tam siyahısı. Backend tərəfində ayrıca
// admin endpoint-i qurmağa ehtiyac yoxdur — SecurityConfig-də
// "/api/dispatcher/**" artıq hasAnyRole("DISPATCHER","ADMIN") ilə açıqdır
// (bax DispatcherController#liveTrips), ona görə eyni zənginləşdirilmiş
// endpoint birbaşa buradan da çağırılır. "Ətraflı bax" mövcud TripDetailModal-ı
// açır (real yol marşrutu xəritədə, xərclər, mərhələ zolağı).
export default function AdminTrips() {
  const { t } = useTranslation();
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [customerModalTrip, setCustomerModalTrip] = useState(null);
  const [borderTrip, setBorderTrip] = useState(null);
  const [detailTrackingNumber, setDetailTrackingNumber] = useState(null);

  const load = () => {
    axiosClient.get('/api/dispatcher/trips/live')
      .then((res) => setTrips([...res.data].sort((a, b) => b.tripId - a.tripId)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <div className="flex-between">
        <div>
          <h2>{t('dispatcher.tripsTitle')}</h2>
          <p>{t('admin.tripsDesc', { count: trips.length })}</p>
        </div>
        <button
          type="button"
          className="btn btn-sm flex items-center gap-1.5"
          onClick={() => downloadCsv('/api/admin/export/trips.csv', 'reysler.csv')}
        >
          <FileDown size={14} /> {t('admin.exportBtn')}
        </button>
      </div>

      <div className="table-wrap mt-16">
        <table>
          <thead>
            <tr>
              <th>{t('dispatcher.colId')}</th><th>{t('dispatcher.colDriver')}</th><th>{t('dispatcher.colVehicle')}</th><th>{t('dispatcher.colCustomer')}</th><th>{t('dispatcher.colRoute')}</th><th>{t('dispatcher.colStatus')}</th>
              <th>{t('admin.colPickedUp')}</th><th>{t('admin.colDelivered')}</th><th>{t('dispatcher.colDistance')}</th><th>{t('dispatcher.colCost')}</th><th>{t('dispatcher.colBorder')}</th><th>{t('dispatcher.colDetail')}</th>
            </tr>
          </thead>
          <tbody>
            {trips.map((tr) => (
              <tr key={tr.tripId}>
                <td>{tr.tripId}</td>
                <td>{tr.driverName || '—'}</td>
                <td>{tr.vehiclePlate || '—'}</td>
                <td>
                  {tr.customers?.length > 0 ? (
                    <button
                      type="button"
                      onClick={() => setCustomerModalTrip(tr)}
                      className="btn btn-sm"
                      style={{ borderColor: 'transparent', background: 'transparent', color: 'var(--primary)', padding: '4px 6px' }}
                    >
                      <span className="flex items-center gap-1">
                        <Users size={13} />
                        {tr.customers[0].fullName || t('dispatcher.unknown')}
                        {tr.customers.length > 1 ? ` +${tr.customers.length - 1}` : ''}
                      </span>
                    </button>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </td>
                <td>{tr.routeInfo || '—'}</td>
                <td><span className={`badge ${STATUS_KEY[tr.status]?.className}`}>{STATUS_KEY[tr.status] ? t(`driver.${STATUS_KEY[tr.status].key}`) : tr.status}</span></td>
                <td style={{ fontSize: 12.5 }}>{formatDate(tr.startedAt)}</td>
                <td style={{ fontSize: 12.5 }}>{formatDate(tr.deliveredAt)}</td>
                <td>{tr.estimatedDistanceKm != null ? `${tr.estimatedDistanceKm} km` : '—'}</td>
                <td>{tr.estimatedCost != null ? `${tr.estimatedCost} ₼` : '—'}</td>
                <td>
                  <button type="button" onClick={() => setBorderTrip(tr)} className="btn btn-sm" style={{ gap: 5 }}>
                    <Flag size={12} /> {t('dispatcher.crossingsBtn')}
                  </button>
                </td>
                <td>
                  {tr.customers?.length > 0 ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      {tr.customers.map((c, i) => (
                        <button
                          key={c.trackingNumber || i}
                          type="button"
                          onClick={() => setDetailTrackingNumber(c.trackingNumber)}
                          disabled={!c.trackingNumber}
                          className="btn btn-sm"
                          style={{ gap: 5 }}
                        >
                          <Search size={12} /> {tr.customers.length > 1 ? c.trackingNumber : t('driver.detailBtn')}
                        </button>
                      ))}
                    </div>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </td>
              </tr>
            ))}
            {trips.length === 0 && (
              <tr><td colSpan={12} className="text-center text-muted">{t('dispatcher.noTrips')}</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <CustomerInfoModal customers={customerModalTrip?.customers} onClose={() => setCustomerModalTrip(null)} onPaid={load} />
      {borderTrip && <TripBorderPanel trip={borderTrip} onClose={() => setBorderTrip(null)} />}
      <TripDetailModal trackingNumber={detailTrackingNumber} onClose={() => setDetailTrackingNumber(null)} />
    </div>
  );
}
