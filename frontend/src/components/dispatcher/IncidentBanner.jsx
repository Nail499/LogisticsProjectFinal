// Sürücünün yolda bildirdiyi fövqəladə hallar (qəza/sınma/yol bağlanması/
// digər) Control Tower-da — bax DriverCurrentTrip.jsx-dəki bildirim forması,
// backend DriverController#reportIncident. FatigueAlertBanner ilə eyni
// naxış, sadəcə qırmızı (təhlükəsizliklə bağlı, maliyyə anomaliyasından və
// yorğunluqdan daha təcili) rəngdə.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, Check, ImageIcon } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

// Backend "/uploads/..." kimi nisbi yol qaytarır — VehiclePhotosCard.jsx-də
// istifadə olunan eyni naxış (bax orada API_BASE qeydi).
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const TYPE_KEY = {
  ACCIDENT: 'incidentAccident',
  BREAKDOWN: 'incidentBreakdown',
  ROAD_CLOSURE: 'incidentRoadClosure',
  OTHER: 'incidentOther',
};

export default function IncidentBanner() {
  const { t } = useTranslation();
  const [incidents, setIncidents] = useState([]);

  const load = () => {
    axiosClient.get('/api/dispatcher/incidents')
      .then((res) => setIncidents(res.data || []))
      .catch(() => setIncidents([]));
  };

  useEffect(() => {
    load();
    const id = setInterval(load, 20000);
    return () => clearInterval(id);
  }, []);

  const resolve = async (id) => {
    try {
      await axiosClient.post(`/api/dispatcher/incidents/${id}/resolve`);
      setIncidents((prev) => prev.filter((i) => i.id !== id));
    } catch {
      /* ignore */
    }
  };

  if (incidents.length === 0) return null;

  return (
    <div className="mb-6" style={{ overflow: 'hidden', borderRadius: 14, border: '1px solid rgba(220,38,38,0.35)', background: 'linear-gradient(to right, rgba(220,38,38,0.10), rgba(220,38,38,0.02))' }}>
      <div className="flex items-start gap-3" style={{ padding: 16 }}>
        <div style={{ display: 'flex', height: 36, width: 36, flexShrink: 0, alignItems: 'center', justifyContent: 'center', borderRadius: '50%', background: 'rgba(220,38,38,0.18)', color: '#b91c1c' }}>
          <AlertTriangle size={18} />
        </div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <h4 style={{ margin: 0, fontSize: 14, fontWeight: 700, color: '#b91c1c' }}>{t('dispatcher.incidentBannerTitle')}</h4>
          <p className="text-muted" style={{ margin: '2px 0 0', fontSize: 12 }}>{t('dispatcher.incidentBannerDesc')}</p>

          <div className="mt-8" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {incidents.map((inc) => (
              <div
                key={inc.id}
                style={{ borderRadius: 8, border: '1px solid rgba(220,38,38,0.25)', background: '#fef2f2', padding: '8px 12px', fontSize: 12.5 }}
              >
                <div className="flex-between" style={{ alignItems: 'flex-start' }}>
                  <span>
                    <span style={{ fontWeight: 700, color: '#b91c1c' }}>{TYPE_KEY[inc.type] ? t(`driver.${TYPE_KEY[inc.type]}`) : inc.type}</span>
                    {' · '}{inc.driverName || '—'}
                    {' · '}{inc.vehiclePlate || '—'}
                    {' · '}{t('dispatcher.colId')} #{inc.trip?.id ?? '—'}
                  </span>
                  <button
                    type="button"
                    onClick={() => resolve(inc.id)}
                    className="flex items-center gap-1"
                    style={{ borderRadius: 6, border: '1px solid rgba(220,38,38,0.4)', padding: '4px 8px', fontSize: 11, fontWeight: 600, color: '#b91c1c', background: 'transparent', flexShrink: 0 }}
                  >
                    <Check size={11} /> {t('dispatcher.incidentResolve')}
                  </button>
                </div>
                {inc.description && (
                  <p style={{ margin: '6px 0 0', color: '#7f1d1d' }}>{inc.description}</p>
                )}
                {inc.photoUrl && (
                  <a
                    href={API_BASE + inc.photoUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center gap-1"
                    style={{ marginTop: 6, color: '#b91c1c', fontWeight: 600, fontSize: 11.5 }}
                  >
                    <ImageIcon size={12} /> {t('dispatcher.incidentPhoto')}
                  </a>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
