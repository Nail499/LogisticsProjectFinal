import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';
import AnimatedCounter from '../../components/AnimatedCounter.jsx';

export default function AdminHome() {
  const { t } = useTranslation();
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    axiosClient.get('/api/admin/reports/summary')
      .then((res) => setSummary(res.data))
      .catch(() => setError(t('admin.errLoadSummary')));
  }, [t]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!summary) return <p>{t('common.loading')}</p>;

  const cards = [
    { label: t('admin.statDrivers'), value: summary.totalDrivers },
    { label: t('admin.statVehicles'), value: summary.totalVehicles },
    { label: t('admin.statTotalTrips'), value: summary.totalTrips },
    { label: t('admin.statDeliveredTrips'), value: summary.deliveredTrips },
    { label: t('admin.statPendingApplications'), value: summary.pendingApplications },
    { label: t('admin.statPendingCargo'), value: summary.pendingCargo },
    { label: t('admin.statTotalExpenses'), value: summary.totalExpenses, decimals: 2 },
    { label: t('admin.statAnomalies'), value: summary.anomalyCount, alert: summary.anomalyCount > 0 },
  ];

  return (
    <div>
      <Reveal>
        <h2>{t('admin.homeTitle')}</h2>
        <p>{t('admin.homeDesc')}</p>
      </Reveal>
      <div className="grid grid-4 mt-24">
        {cards.map((c, i) => (
          <Reveal key={c.label} delay={i * 60}>
            <div className={`stat-card hover-lift${c.alert ? ' stat-card-alert' : ''}`}>
              <div className="stat-label">{c.label}</div>
              <div className="stat-value">
                <AnimatedCounter to={c.value} decimals={c.decimals || 0} />
              </div>
            </div>
          </Reveal>
        ))}
      </div>
    </div>
  );
}
