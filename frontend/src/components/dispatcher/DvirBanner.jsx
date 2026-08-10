// Reys öncəsi/sonrası yoxlama siyahısında (DVIR) sürücünün qeyd etdiyi
// defektlər — bax DriverCurrentTrip.jsx-dəki DVIR forması, backend
// DriverController#submitDvir. IncidentBanner ilə eyni naxış.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ClipboardX, Check } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

const TYPE_KEY = {
  PRE_TRIP: 'dvirPreTrip',
  POST_TRIP: 'dvirPostTrip',
};

export default function DvirBanner() {
  const { t } = useTranslation();
  const [inspections, setInspections] = useState([]);

  const load = () => {
    axiosClient.get('/api/dispatcher/dvir')
      .then((res) => setInspections(res.data || []))
      .catch(() => setInspections([]));
  };

  useEffect(() => {
    load();
    const id = setInterval(load, 20000);
    return () => clearInterval(id);
  }, []);

  const resolve = async (id) => {
    try {
      await axiosClient.post(`/api/dispatcher/dvir/${id}/resolve`);
      setInspections((prev) => prev.filter((i) => i.id !== id));
    } catch {
      /* ignore */
    }
  };

  if (inspections.length === 0) return null;

  return (
    <div className="mb-6" style={{ overflow: 'hidden', borderRadius: 14, border: '1px solid rgba(220,38,38,0.35)', background: 'linear-gradient(to right, rgba(220,38,38,0.10), rgba(220,38,38,0.02))' }}>
      <div className="flex items-start gap-3" style={{ padding: 16 }}>
        <div style={{ display: 'flex', height: 36, width: 36, flexShrink: 0, alignItems: 'center', justifyContent: 'center', borderRadius: '50%', background: 'rgba(220,38,38,0.18)', color: '#b91c1c' }}>
          <ClipboardX size={18} />
        </div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <h4 style={{ margin: 0, fontSize: 14, fontWeight: 700, color: '#b91c1c' }}>{t('dispatcher.dvirBannerTitle')}</h4>
          <p className="text-muted" style={{ margin: '2px 0 0', fontSize: 12 }}>{t('dispatcher.dvirBannerDesc')}</p>

          <div className="mt-8" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {inspections.map((insp) => (
              <div
                key={insp.id}
                style={{ borderRadius: 8, border: '1px solid rgba(220,38,38,0.25)', background: '#fef2f2', padding: '8px 12px', fontSize: 12.5 }}
              >
                <div className="flex-between" style={{ alignItems: 'flex-start' }}>
                  <span>
                    <span style={{ fontWeight: 700, color: '#b91c1c' }}>{TYPE_KEY[insp.type] ? t(`dispatcher.${TYPE_KEY[insp.type]}`) : insp.type}</span>
                    {' · '}{insp.driverName || '—'}
                    {' · '}{insp.vehiclePlate || '—'}
                    {' · '}{t('dispatcher.colId')} #{insp.trip?.id ?? '—'}
                  </span>
                  <button
                    type="button"
                    onClick={() => resolve(insp.id)}
                    className="flex items-center gap-1"
                    style={{ borderRadius: 6, border: '1px solid rgba(220,38,38,0.4)', padding: '4px 8px', fontSize: 11, fontWeight: 600, color: '#b91c1c', background: 'transparent', flexShrink: 0 }}
                  >
                    <Check size={11} /> {t('dispatcher.incidentResolve')}
                  </button>
                </div>
                {insp.items && (
                  <p style={{ margin: '6px 0 0', color: '#7f1d1d' }}>
                    {Object.entries(insp.items)
                      .filter(([, status]) => status === 'DEFECT')
                      .map(([key]) => t(`driver.dvirItem_${key}`, key))
                      .join(', ')}
                  </p>
                )}
                {insp.notes && (
                  <p style={{ margin: '4px 0 0', color: '#7f1d1d' }}>{insp.notes}</p>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
