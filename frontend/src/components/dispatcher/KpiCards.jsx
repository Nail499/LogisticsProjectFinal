// Xüsusiyyət 4 — Dispetçer KPI paneli. AnalyticsCharts.jsx-in bacısı
// komponent: eyni səhifədə (ControlTower), eyni fetch-then-render
// naxışı ilə /api/dispatcher/reports/kpi-dən DispatcherKpiResponse
// göstərir. onTimePercent və deadheadPercent real ölçmə deyil, sənədləşdirilmiş
// təxminlərdir (bax backend DispatcherKpiResponse.java şərhi) — bunu UI-da
// da açıq işarələyirik ki, dispetçer bu rəqəmlərə "black box" kimi baxmasın.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Clock, ArrowLeftRight, Truck, Package } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

function KpiCard({ icon: Icon, label, value, hint }) {
  return (
    <div className="stat-card">
      <div className="flex items-center gap-1.5 stat-label">
        <Icon size={13} style={{ color: 'var(--primary)' }} /> {label}
      </div>
      <div className="stat-value">{value}</div>
      {hint && <p className="text-muted" style={{ margin: '4px 0 0', fontSize: 10.5 }}>{hint}</p>}
    </div>
  );
}

export default function KpiCards() {
  const { t } = useTranslation();
  const [data, setData] = useState(null);

  useEffect(() => {
    axiosClient.get('/api/dispatcher/reports/kpi')
      .then((res) => setData(res.data))
      .catch(() => setData(null));
  }, []);

  if (!data) return null;

  const naText = t('dispatcher.kpiNoData');
  const pct = (v) => (v === null || v === undefined ? naText : `${v}%`);

  return (
    <div className="grid grid-4 mt-24" style={{ gap: 16 }}>
      <KpiCard
        icon={Package}
        label={t('dispatcher.kpiDeliveredTrips')}
        value={data.deliveredTripsCount}
      />
      <KpiCard
        icon={Clock}
        label={t('dispatcher.kpiOnTimePercent')}
        value={pct(data.onTimePercent)}
        hint={t('dispatcher.kpiEstimateHint')}
      />
      <KpiCard
        icon={ArrowLeftRight}
        label={t('dispatcher.kpiDeadheadPercent')}
        value={pct(data.deadheadPercent)}
        hint={t('dispatcher.kpiEstimateHint')}
      />
      <KpiCard
        icon={Truck}
        label={t('dispatcher.kpiTrailerUtilization')}
        value={pct(data.trailerUtilizationPercent)}
        hint={
          data.avgCapacityUtilizationPercent !== null && data.avgCapacityUtilizationPercent !== undefined
            ? `${t('dispatcher.kpiAvgCapacity')}: ${data.avgCapacityUtilizationPercent}%`
            : undefined
        }
      />
    </div>
  );
}
