import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';
import LiveTrackingPanel from '../../components/LiveTrackingPanel.jsx';

export default function LiveTracking() {
  const { t } = useTranslation();
  const { trackingNumber } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError('');
    axiosClient.get(`/api/tracking/${trackingNumber}`)
      .then((res) => setData(res.data))
      .catch(() => setError(t('tracking.notFound')))
      .finally(() => setLoading(false));
  }, [trackingNumber]);

  return (
    <div>
      <Reveal>
        <div className="flex-between">
          <div>
            <h2>{t('tracking.liveTrack')}</h2>
            <p>{trackingNumber}</p>
          </div>
          <Link to="/customer/orders">{t('tracking.backToOrders')}</Link>
        </div>
      </Reveal>

      {loading && <p className="mt-16">{t('common.loading')}</p>}
      {error && <div className="alert alert-error mt-16">{error}</div>}

      {data && (
        <Reveal delay={60}>
          <div className="mt-16">
            <LiveTrackingPanel data={data} />
          </div>
        </Reveal>
      )}
    </div>
  );
}
