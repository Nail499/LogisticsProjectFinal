import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Radio } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import { useAuth } from '../../context/AuthContext.jsx';
import Reveal from '../../components/Reveal.jsx';
import AnimatedCounter from '../../components/AnimatedCounter.jsx';
import OrderTimeline, { STATUS_CLASS } from '../../components/OrderTimeline.jsx';
import { IconPackage, IconClock, IconTruck, IconFileCheck, IconArrowRight, IconMapPin } from '../../components/icons.jsx';

export default function CustomerHome() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axiosClient.get('/api/customer/cargo')
      .then((res) => setOrders(res.data))
      .finally(() => setLoading(false));
  }, []);

  const stats = useMemo(() => {
    const total = orders.length;
    const pending = orders.filter((o) => o.status === 'PENDING').length;
    const transit = orders.filter((o) => o.status === 'ASSIGNED' || o.status === 'IN_TRANSIT').length;
    const delivered = orders.filter((o) => o.status === 'DELIVERED').length;
    return { total, pending, transit, delivered };
  }, [orders]);

  const recent = useMemo(() => [...orders].sort((a, b) => (b.id || 0) - (a.id || 0)).slice(0, 3), [orders]);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <Reveal>
        <div className="welcome-banner">
          <div className="welcome-banner-inner">
            <div>
              <h2>{t('customer.welcomeTitle', { name: user?.username })}</h2>
              <p>{t('customer.welcomeDesc')}</p>
            </div>
            <div className="welcome-banner-actions">
              <Link to="/customer/new" className="btn btn-primary">
                {t('customer.newOrderBtn')} <IconArrowRight width={16} height={16} />
              </Link>
              <Link to="/customer/orders" className="btn btn-ghost-light">{t('customer.allOrdersBtn')}</Link>
            </div>
          </div>
        </div>
      </Reveal>

      <div className="grid grid-4">
        <Reveal delay={40}>
          <div className="cust-stat-card hover-lift">
            <div className="cust-stat-icon total"><IconPackage width={22} height={22} /></div>
            <div>
              <div className="cust-stat-num"><AnimatedCounter to={stats.total} /></div>
              <div className="cust-stat-label">{t('customer.statTotal')}</div>
            </div>
          </div>
        </Reveal>
        <Reveal delay={90}>
          <div className="cust-stat-card hover-lift">
            <div className="cust-stat-icon pending"><IconClock width={22} height={22} /></div>
            <div>
              <div className="cust-stat-num"><AnimatedCounter to={stats.pending} /></div>
              <div className="cust-stat-label">{t('customer.statPending')}</div>
            </div>
          </div>
        </Reveal>
        <Reveal delay={140}>
          <div className="cust-stat-card hover-lift">
            <div className="cust-stat-icon transit"><IconTruck width={22} height={22} /></div>
            <div>
              <div className="cust-stat-num"><AnimatedCounter to={stats.transit} /></div>
              <div className="cust-stat-label">{t('customer.statTransit')}</div>
            </div>
          </div>
        </Reveal>
        <Reveal delay={190}>
          <div className="cust-stat-card hover-lift">
            <div className="cust-stat-icon delivered"><IconFileCheck width={22} height={22} /></div>
            <div>
              <div className="cust-stat-num"><AnimatedCounter to={stats.delivered} /></div>
              <div className="cust-stat-label">{t('customer.statDelivered')}</div>
            </div>
          </div>
        </Reveal>
      </div>

      <Reveal delay={220}>
        <div className="mt-24">
          <div className="flex-between mt-24" style={{ marginBottom: 14 }}>
            <h3 style={{ margin: 0 }}>{t('customer.recentOrders')}</h3>
            {orders.length > 0 && <Link to="/customer/orders">{t('customer.viewAll')}</Link>}
          </div>

          {recent.length === 0 ? (
            <div className="card empty-state">
              <div className="empty-state-icon"><IconPackage width={72} height={72} /></div>
              <h3>{t('customer.noOrdersTitle')}</h3>
              <p className="text-muted">{t('customer.noOrdersDesc')}</p>
              <Link to="/customer/new" className="btn btn-primary mt-16">{t('customer.placeOrderBtn')}</Link>
            </div>
          ) : (
            recent.map((o) => (
              <div className="order-card hover-lift" key={o.id}>
                <div className="order-card-head">
                  <div>
                    <div className="order-track-num">{o.trackingNumber}</div>
                    <p className="order-desc">{o.description}</p>
                    <div className="order-meta">
                      <span><IconMapPin width={13} height={13} /> {o.destinationAddress}</span>
                      {o.cargoType && <span className={`cargo-type-chip cargo-type-${o.cargoType.toLowerCase()}`}>{o.cargoType}</span>}
                      {o.urgency && <span className={`cargo-type-chip urgency-${o.urgency.toLowerCase()}`}>{o.urgency}</span>}
                    </div>
                  </div>
                  <span className={`badge ${STATUS_CLASS[o.status]}`}>{t(`status.${o.status}`)}</span>
                </div>
                <OrderTimeline status={o.status} />
                <Link
                  to={`/customer/track/${o.trackingNumber}`}
                  className="mt-3 inline-flex items-center gap-1.5 text-xs font-semibold text-accent-blue hover:underline"
                >
                  <Radio size={13} /> {t('tracking.liveTrack')}
                </Link>
              </div>
            ))
          )}
        </div>
      </Reveal>
    </div>
  );
}
