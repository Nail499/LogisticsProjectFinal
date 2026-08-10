// Stage 4 — Control Tower anomaly banner. Pulls TripExpense rows flagged
// isAnomaly=true (Z-Score logic lives server-side, see
// AdminReportService#getAnomalies) via the dispatcher-scoped wrapper
// endpoint, since /api/admin/** is off-limits for the DISPATCHER role.
// Restyled to the site's light Fleetra theme; each anomaly is now shown as
// a detailed row (which trip, route, driver, vehicle, date, description)
// instead of a compact chip, so the dispatcher can see exactly what/who a
// suspicious expense belongs to without leaving this page.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ShieldAlert, X, Banknote, Route, Truck, User, Calendar, Receipt } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

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

export default function AnomalyBanner() {
  const { t, i18n } = useTranslation();
  const [anomalies, setAnomalies] = useState([]);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    axiosClient.get('/api/dispatcher/reports/anomalies')
      .then((res) => setAnomalies(res.data || []))
      .catch(() => setAnomalies([]));
  }, []);

  if (dismissed || anomalies.length === 0) return null;

  return (
    <div className="mb-6" style={{ overflow: 'hidden', borderRadius: 14, border: '1px solid rgba(220,38,38,0.35)', background: 'linear-gradient(to right, rgba(220,38,38,0.08), rgba(220,38,38,0.01))' }}>
      <div style={{ padding: 16 }}>
        <div className="flex items-start gap-3">
          <div style={{ display: 'flex', height: 36, width: 36, flexShrink: 0, alignItems: 'center', justifyContent: 'center', borderRadius: '50%', background: 'rgba(220,38,38,0.15)', color: 'var(--danger)' }}>
            <ShieldAlert size={18} />
          </div>
          <div style={{ minWidth: 0, flex: 1 }}>
            <div className="flex-between">
              <h4 style={{ margin: 0, fontSize: 14, fontWeight: 700, color: 'var(--danger)' }}>
                {t('dispatcher.anomalyTitle', { count: anomalies.length })}
              </h4>
              <button
                type="button"
                onClick={() => setDismissed(true)}
                className="btn btn-sm"
                style={{ padding: 5 }}
              >
                <X size={14} />
              </button>
            </div>
            <p className="text-muted" style={{ margin: '2px 0 0', fontSize: 12 }}>{t('dispatcher.anomalyDesc')}</p>
          </div>
        </div>

        <div className="mt-16" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {anomalies.map((a) => (
            <div
              key={a.id}
              style={{ borderRadius: 10, border: '1px solid rgba(220,38,38,0.25)', background: '#fff', padding: 12 }}
            >
              <div className="flex-between" style={{ flexWrap: 'wrap', gap: 8 }}>
                <div className="flex items-center gap-1.5">
                  <Banknote size={14} style={{ color: 'var(--danger)' }} />
                  <span style={{ fontWeight: 700, color: 'var(--danger)', fontSize: 14 }}>{a.amount?.toFixed(2)} ₼</span>
                  <span className="badge badge-danger">{CATEGORY_KEY[a.category] ? t(`driver.${CATEGORY_KEY[a.category]}`) : a.category}</span>
                  {a.tripId && <span className="badge badge-neutral">{t('dispatcher.tripFallback', { id: a.tripId })}</span>}
                  {a.tripStatus && <span className="badge badge-info">{a.tripStatus}</span>}
                  {typeof a.percentAboveAverage === 'number' && (
                    <span className="badge badge-danger">{t('dispatcher.aboveAverage', { pct: a.percentAboveAverage })}</span>
                  )}
                </div>
                {a.recordedAt && (
                  <span className="flex items-center gap-1 text-muted" style={{ fontSize: 11.5 }}>
                    <Calendar size={12} /> {new Date(a.recordedAt).toLocaleString(i18n.language)}
                  </span>
                )}
              </div>

              {a.description && (
                <p style={{ margin: '8px 0 0', fontSize: 12.5 }}>{a.description}</p>
              )}

              <div className="flex items-center gap-16 mt-8" style={{ flexWrap: 'wrap', fontSize: 12 }}>
                {(a.pickupAddress || a.destinationAddress) && (
                  <span className="flex items-center gap-1 text-muted">
                    <Route size={13} /> {[a.pickupAddress, a.destinationAddress].filter(Boolean).join(' → ')}
                  </span>
                )}
                {a.driverName && (
                  <span className="flex items-center gap-1 text-muted">
                    <User size={13} /> {a.driverName}
                  </span>
                )}
                {a.vehiclePlate && (
                  <span className="flex items-center gap-1 text-muted">
                    <Truck size={13} /> {a.vehiclePlate}
                  </span>
                )}
                {a.receiptPhotoUrl && (
                  <a
                    href={API_BASE + a.receiptPhotoUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center gap-1"
                    style={{ color: 'var(--primary)' }}
                  >
                    <Receipt size={13} /> {t('admin.viewReceiptBtn')}
                  </a>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
