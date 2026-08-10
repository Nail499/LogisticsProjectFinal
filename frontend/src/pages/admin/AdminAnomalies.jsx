import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, User, Truck, MapPin, Calendar, TrendingUp, Search, Receipt } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import TripDetailModal from '../../components/TripDetailModal.jsx';

// Qəbz fotosu (əgər sürücü yükləyibsə) — bax
// DriverController#addExpenseWithReceipt, TripExpense.receiptPhotoUrl.
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const CATEGORY_KEY = {
  FUEL: 'categoryFuel',
  TOLL: 'categoryToll',
  FOOD: 'categoryFood',
  MAINTENANCE: 'categoryMaintenance',
  OTHER: 'categoryOther',
};

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('az-AZ', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ', ' + d.toLocaleTimeString('az-AZ', { hour: '2-digit', minute: '2-digit' });
}

// Əvvəllər bare TripExpense (yalnız trip.id) cədvəli idi — admin xərcin hansı
// reysə, sürücüyə, müştəriyə aid olduğunu görə bilmirdi. İndi backend
// zənginləşdirilmiş AnomalyExpenseResponse qaytarır (bax
// AdminReportService#buildAnomalyResponse): sürücü/nəqliyyat vasitəsi,
// götürülmə/çatdırılma ünvanları, müştəri(lər), kateqoriya ortalamasından
// neçə faiz yuxarı olduğu — və "Ətraflı bax" düyməsi TripDetailModal-ı açır
// (mövcud LiveTrackingPanel — real yol marşrutu xəritədə).
export default function AdminAnomalies() {
  const { t } = useTranslation();
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [detailTrackingNumber, setDetailTrackingNumber] = useState(null);

  useEffect(() => {
    axiosClient.get('/api/admin/reports/anomalies')
      .then((res) => setExpenses(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <h2>{t('admin.anomaliesTitle')}</h2>
      <p>{t('admin.anomaliesDesc')}</p>

      {expenses.length === 0 && (
        <div className="card mt-16 text-center text-muted">{t('admin.noAnomalies')}</div>
      )}

      {expenses.map((exp) => (
        <div className="card hover-lift mt-16" key={exp.id}>
          <div className="flex-between" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 10 }}>
            <div style={{ minWidth: 0 }}>
              <div className="flex items-center gap-1.5 text-xs text-muted">
                <AlertTriangle size={12} style={{ color: 'var(--danger)' }} /> {t('dispatcher.tripFallback', { id: exp.tripId ?? '—' })} · {CATEGORY_KEY[exp.category] ? t(`driver.${CATEGORY_KEY[exp.category]}`) : exp.category}
              </div>
              <h3 style={{ margin: '4px 0 0', color: 'var(--danger)' }}>{exp.amount} ₼</h3>
              {exp.description && (
                <p className="text-muted" style={{ margin: '4px 0 0', fontSize: 13 }}>{exp.description}</p>
              )}
            </div>
            <div style={{ textAlign: 'right', flexShrink: 0 }}>
              <span className="badge badge-danger">
                <TrendingUp size={12} style={{ marginRight: 4 }} />
                {t('dispatcher.aboveAverage', { pct: exp.percentAboveAverage > 0 ? `+${exp.percentAboveAverage}` : exp.percentAboveAverage })}
              </span>
              <p className="text-muted" style={{ margin: '4px 0 0', fontSize: 12 }}>
                {t('admin.categoryAverage', { amount: exp.categoryAverageAmount })}
              </p>
            </div>
          </div>

          <div className="flex mt-16" style={{ flexWrap: 'wrap', gap: 16, border: '1px solid #e5e7eb', borderRadius: 10, padding: 12, fontSize: 13 }}>
            <span className="flex items-center gap-1.5"><Calendar size={13} style={{ color: 'var(--primary)' }} /> {formatDate(exp.recordedAt)}</span>
            {exp.driverName && (
              <span className="flex items-center gap-1.5"><User size={13} style={{ color: 'var(--primary)' }} /> {exp.driverName}</span>
            )}
            {exp.vehiclePlate && (
              <span className="flex items-center gap-1.5"><Truck size={13} style={{ color: 'var(--primary)' }} /> {exp.vehiclePlate}</span>
            )}
          </div>

          {(exp.pickupAddress || exp.destinationAddress) && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 3, marginTop: 10, fontSize: 13 }}>
              {exp.pickupAddress && (
                <span className="flex items-center gap-1.5 text-muted">🟢 {exp.pickupAddress}</span>
              )}
              {exp.destinationAddress && (
                <span className="flex items-center gap-1.5 text-muted">
                  <MapPin size={13} style={{ color: 'var(--primary)', flexShrink: 0 }} /> {exp.destinationAddress}
                </span>
              )}
            </div>
          )}

          {(exp.customers && exp.customers.length > 0) || exp.receiptPhotoUrl ? (
            <div className="mt-16" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {exp.customers && exp.customers.map((c, i) => (
                <button
                  key={c.trackingNumber || i}
                  type="button"
                  className="btn btn-sm"
                  onClick={() => setDetailTrackingNumber(c.trackingNumber)}
                  disabled={!c.trackingNumber}
                  style={{ gap: 6 }}
                >
                  <Search size={13} /> {t('admin.viewMapBtn')}
                  {exp.customers.length > 1 ? ` — ${c.fullName || c.trackingNumber}` : ''}
                </button>
              ))}
              {exp.receiptPhotoUrl && (
                <a
                  href={API_BASE + exp.receiptPhotoUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="btn btn-sm"
                  style={{ gap: 6 }}
                >
                  <Receipt size={13} /> {t('admin.viewReceiptBtn')}
                </a>
              )}
            </div>
          ) : null}
        </div>
      ))}

      <TripDetailModal trackingNumber={detailTrackingNumber} onClose={() => setDetailTrackingNumber(null)} />
    </div>
  );
}
