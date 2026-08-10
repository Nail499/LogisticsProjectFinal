// Stage 4 — Dispatcher "Control Tower": global truck map, closest-warehouse
// lookup, crimson anomaly banner, backhaul/empty-miles matcher and Recharts
// analytics, all in one dark command-center page.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { RadioTower } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import AnomalyBanner from '../../components/dispatcher/AnomalyBanner.jsx';
import FatigueAlertBanner from '../../components/dispatcher/FatigueAlertBanner.jsx';
import IncidentBanner from '../../components/dispatcher/IncidentBanner.jsx';
import DvirBanner from '../../components/dispatcher/DvirBanner.jsx';
import ControlTowerMap from '../../components/dispatcher/ControlTowerMap.jsx';
import BackhaulMatcher from '../../components/dispatcher/BackhaulMatcher.jsx';
import AnalyticsCharts from '../../components/dispatcher/AnalyticsCharts.jsx';
import KpiCards from '../../components/dispatcher/KpiCards.jsx';
import { subscribeTopic } from '../../utils/socket.js';

export default function ControlTower() {
  const { t } = useTranslation();
  const [liveTrips, setLiveTrips] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [pendingCargo, setPendingCargo] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    Promise.all([
      axiosClient.get('/api/dispatcher/trips/live'),
      axiosClient.get('/api/dispatcher/warehouses'),
      axiosClient.get('/api/dispatcher/cargo/pending'),
    ])
      .then(([tripsRes, whRes, cargoRes]) => {
        setLiveTrips(tripsRes.data || []);
        setWarehouses(whRes.data || []);
        setPendingCargo(cargoRes.data || []);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // Stage 6: the 60s poll is now just a safety-net resync (covers new
    // trips being created, cargo picked up, etc.) — actual position/status
    // changes arrive instantly below via WebSocket.
    const id = setInterval(load, 60000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    // Stage 6 — live GPS/status push (see backend TripBroadcastService).
    // Each message is one enriched trip; merge it into the existing list
    // by tripId (update in place, or append if it's a trip we haven't
    // seen yet — e.g. right after a new trip is created).
    const unsubscribe = subscribeTopic('/topic/dispatcher/live-trips', (updatedTrip) => {
      setLiveTrips((prev) => {
        const idx = prev.findIndex((t) => t.tripId === updatedTrip.tripId);
        if (idx === -1) return [...prev, updatedTrip];
        const next = [...prev];
        next[idx] = updatedTrip;
        return next;
      });
    });
    return unsubscribe;
  }, []);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <div className="flex items-center gap-2" style={{ marginBottom: 16 }}>
        <RadioTower size={20} style={{ color: 'var(--primary)' }} />
        <div>
          <h2 style={{ margin: 0 }}>{t('dispatcher.controlTowerTitle')}</h2>
          <p style={{ margin: 0 }}>{t('dispatcher.controlTowerDesc')}</p>
        </div>
      </div>

      <IncidentBanner />
      <DvirBanner />
      <AnomalyBanner />
      <FatigueAlertBanner />
      <ControlTowerMap liveTrips={liveTrips} warehouses={warehouses} />
      <BackhaulMatcher liveTrips={liveTrips} pendingCargo={pendingCargo} />
      <KpiCards />
      <AnalyticsCharts />
    </div>
  );
}
