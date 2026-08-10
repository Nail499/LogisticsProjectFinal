// Stage 5 — surfaces driver "Rest Mode" fatigue warnings (4.5h+ continuous
// driving, see DriverCurrentTrip's RestModeCard) on the Control Tower.
// Amber, not crimson — distinct from the expense-anomaly banner so
// dispatchers can tell financial vs. safety alerts apart at a glance.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BedDouble, Check } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

export default function FatigueAlertBanner() {
  const { t } = useTranslation();
  const [alerts, setAlerts] = useState([]);

  const load = () => {
    axiosClient.get('/api/dispatcher/fatigue-alerts')
      .then((res) => setAlerts(res.data || []))
      .catch(() => setAlerts([]));
  };

  useEffect(() => {
    load();
    const id = setInterval(load, 20000);
    return () => clearInterval(id);
  }, []);

  const resolve = async (id) => {
    try {
      await axiosClient.post(`/api/dispatcher/fatigue-alerts/${id}/resolve`);
      setAlerts((prev) => prev.filter((a) => a.id !== id));
    } catch {
      /* ignore */
    }
  };

  if (alerts.length === 0) return null;

  return (
    <div className="mb-6" style={{ overflow: 'hidden', borderRadius: 14, border: '1px solid rgba(217,119,6,0.35)', background: 'linear-gradient(to right, rgba(217,119,6,0.10), rgba(217,119,6,0.02))' }}>
      <div className="flex items-start gap-3" style={{ padding: 16 }}>
        <div style={{ display: 'flex', height: 36, width: 36, flexShrink: 0, alignItems: 'center', justifyContent: 'center', borderRadius: '50%', background: 'rgba(217,119,6,0.18)', color: '#b45309' }}>
          <BedDouble size={18} />
        </div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <h4 style={{ margin: 0, fontSize: 14, fontWeight: 700, color: '#b45309' }}>{t('dispatcher.fatigueTitle')}</h4>
          <p className="text-muted" style={{ margin: '2px 0 0', fontSize: 12 }}>{t('dispatcher.fatigueDesc')}</p>

          <div className="mt-8" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {alerts.map((a) => (
              <div
                key={a.id}
                className="flex-between"
                style={{ borderRadius: 8, border: '1px solid rgba(217,119,6,0.25)', background: '#fffbeb', padding: '8px 12px', fontSize: 12.5 }}
              >
                <span>
                  <span style={{ fontWeight: 600, color: '#b45309' }}>{a.driverName || '—'}</span>
                  {' · '}{a.vehiclePlate || '—'}
                  {' · '}
                  <span style={{ fontWeight: 700, color: '#b45309' }}>{a.continuousDrivingHours?.toFixed(1)} saat</span>
                </span>
                <button
                  type="button"
                  onClick={() => resolve(a.id)}
                  className="flex items-center gap-1"
                  style={{ borderRadius: 6, border: '1px solid rgba(217,119,6,0.4)', padding: '4px 8px', fontSize: 11, fontWeight: 600, color: '#b45309', background: 'transparent' }}
                >
                  <Check size={11} /> {t('dispatcher.fatigueResolve')}
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
