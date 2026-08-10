import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Users, Flag, Search, MessageCircle, XCircle } from 'lucide-react';
import PaymentStatusBadge from '../../components/dispatcher/PaymentStatusBadge.jsx';
import axiosClient from '../../api/axiosClient';
import CustomerInfoModal from '../../components/dispatcher/CustomerInfoModal.jsx';
import TripBorderPanel from '../../components/dispatcher/TripBorderPanel.jsx';
import TripDetailModal from '../../components/TripDetailModal.jsx';
import DriverChatModal from '../../components/dispatcher/DriverChatModal.jsx';

const STATUS_CLASS = {
  AWAITING_PAYMENT: 'badge-warning',
  PENDING_ACCEPTANCE: 'badge-warning',
  PLANNED: 'badge-neutral',
  PICKED_UP: 'badge-info',
  IN_TRANSIT: 'badge-info',
  DELIVERED: 'badge-success',
  REJECTED: 'badge-danger',
  CANCELLED: 'badge-danger',
};
const STATUS_KEY = {
  AWAITING_PAYMENT: 'statusAwaitingPayment',
  PENDING_ACCEPTANCE: 'statusPendingAcceptance',
  PLANNED: 'statusPlanned',
  PICKED_UP: 'statusPickedUp',
  IN_TRANSIT: 'statusInTransit',
  DELIVERED: 'statusDelivered',
  REJECTED: 'statusRejected',
  CANCELLED: 'statusCancelled',
};
const CANCELLABLE_STATUSES = ['AWAITING_PAYMENT', 'PENDING_ACCEPTANCE'];

export default function DispatcherTrips() {
  const { t } = useTranslation();
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [customerModalTrip, setCustomerModalTrip] = useState(null);
  const [borderTrip, setBorderTrip] = useState(null);
  const [detailTrackingNumber, setDetailTrackingNumber] = useState(null);
  const [driverChatTrip, setDriverChatTrip] = useState(null);
  // Sürücü uzun müddət qəbul/imtina etmirsə (və ya ödəniş çox gözləyirsə),
  // dispetçer cavabı gözləmədən reysi əl ilə ləğv edə bilsin (bax
  // DispatcherController#cancelTrip).
  const [cancellingId, setCancellingId] = useState(null);
  const [message, setMessage] = useState('');

  const load = () => {
    // Stage 4: enriched endpoint (driver/vehicle display names already
    // flattened + destination coords) — same data source the Control Tower
    // map and backhaul matcher use, so this table stays in sync with them.
    // Stage: now also carries a `customers` list (see LiveTripResponse /
    // CustomerSummary) so this table can show/click who the load is for.
    axiosClient.get('/api/dispatcher/trips/live')
      .then((res) => setTrips(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const cancelTrip = async (tripId) => {
    if (!window.confirm(t('dispatcher.cancelTripConfirm'))) return;
    setCancellingId(tripId);
    try {
      await axiosClient.post(`/api/dispatcher/trips/${tripId}/cancel`);
      setMessage(t('dispatcher.tripCancelled'));
      load();
    } catch (err) {
      setMessage(err.response?.data?.message || t('dispatcher.tripCancelError'));
    } finally {
      setCancellingId(null);
    }
  };

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <h2>{t('dispatcher.tripsTitle')}</h2>
      <p>{t('dispatcher.tripsDesc')}</p>

      {message && <div className="alert alert-success mt-16">{message}</div>}

      <div className="table-wrap mt-16">
        <table>
          <thead>
            <tr>
              <th>{t('dispatcher.colId')}</th><th>{t('dispatcher.colDriver')}</th><th>{t('dispatcher.colVehicle')}</th><th>{t('dispatcher.colCustomer')}</th><th>{t('dispatcher.colRoute')}</th><th>{t('dispatcher.colStatus')}</th><th>{t('dispatcher.colDistance')}</th><th>{t('dispatcher.colCost')}</th><th>{t('dispatcher.colBorder')}</th><th>{t('dispatcher.colDriverChat')}</th><th>{t('dispatcher.colDetail')}</th><th>{t('dispatcher.colActions')}</th>
            </tr>
          </thead>
          <tbody>
            {trips.map((t2) => (
              <tr key={t2.tripId}>
                <td>{t2.tripId}</td>
                <td>{t2.driverName || '—'}</td>
                <td>{t2.vehiclePlate || '—'}</td>
                <td>
                  {t2.customers?.length > 0 ? (
                    <button
                      type="button"
                      onClick={() => setCustomerModalTrip(t2)}
                      className="btn btn-sm"
                      style={{ borderColor: 'transparent', background: 'transparent', color: 'var(--primary)', padding: '4px 6px' }}
                    >
                      <span className="flex items-center gap-1">
                        <Users size={13} />
                        {t2.customers[0].fullName || t('dispatcher.unknown')}
                        {t2.customers.length > 1 ? ` +${t2.customers.length - 1}` : ''}
                      </span>
                    </button>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </td>
                <td>{t2.routeInfo || '—'}</td>
                <td><span className={`badge ${STATUS_CLASS[t2.status] || 'badge-info'}`}>{STATUS_KEY[t2.status] ? t(`driver.${STATUS_KEY[t2.status]}`) : t2.status}</span></td>
                <td>{t2.estimatedDistanceKm != null ? `${t2.estimatedDistanceKm} km` : '—'}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                    <span>{t2.estimatedCost != null ? `${t2.estimatedCost} ₼` : '—'}</span>
                    <PaymentStatusBadge customers={t2.customers} />
                  </div>
                </td>
                <td>
                  <button type="button" onClick={() => setBorderTrip(t2)} className="btn btn-sm" style={{ gap: 5 }}>
                    <Flag size={12} /> {t('dispatcher.crossingsBtn')}
                  </button>
                </td>
                <td>
                  {t2.driverName && t2.customers?.length > 0 ? (
                    <button type="button" onClick={() => setDriverChatTrip(t2)} className="btn btn-sm" style={{ gap: 5 }}>
                      <MessageCircle size={12} /> {t('driver.chatBtn')}
                    </button>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </td>
                <td>
                  {t2.customers?.length > 0 ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      {t2.customers.map((c, i) => (
                        <button
                          key={c.trackingNumber || i}
                          type="button"
                          onClick={() => setDetailTrackingNumber(c.trackingNumber)}
                          disabled={!c.trackingNumber}
                          className="btn btn-sm"
                          style={{ gap: 5 }}
                        >
                          <Search size={12} /> {t2.customers.length > 1 ? c.trackingNumber : t('driver.detailBtn')}
                        </button>
                      ))}
                    </div>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </td>
                <td>
                  {CANCELLABLE_STATUSES.includes(t2.status) ? (
                    <button
                      type="button"
                      onClick={() => cancelTrip(t2.tripId)}
                      disabled={cancellingId === t2.tripId}
                      className="btn btn-sm"
                      style={{ gap: 5, color: 'var(--danger)', borderColor: 'var(--danger)' }}
                    >
                      <XCircle size={12} /> {t('dispatcher.cancelTripBtn')}
                    </button>
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
      {driverChatTrip && <DriverChatModal trip={driverChatTrip} onClose={() => setDriverChatTrip(null)} />}
    </div>
  );
}
