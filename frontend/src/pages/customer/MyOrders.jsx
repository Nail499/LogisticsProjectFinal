import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Radio, MessageCircle } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';
import OrderTimeline, { STATUS_CLASS } from '../../components/OrderTimeline.jsx';
import TripRating from '../../components/TripRating.jsx';
import OrderPayment from '../../components/OrderPayment.jsx';
import { IconPackage, IconMapPin, IconSearch, IconArrowRight } from '../../components/icons.jsx';

const FILTER_KEYS = ['ALL', 'PENDING', 'ASSIGNED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'];

export default function MyOrders() {
  const { t } = useTranslation();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');

  useEffect(() => {
    axiosClient.get('/api/customer/cargo')
      .then((res) => setOrders(res.data))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    let list = [...orders].sort((a, b) => (b.id || 0) - (a.id || 0));
    if (filter !== 'ALL') list = list.filter((o) => o.status === filter);
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      list = list.filter((o) =>
        o.trackingNumber?.toLowerCase().includes(q) ||
        o.description?.toLowerCase().includes(q) ||
        o.destinationAddress?.toLowerCase().includes(q)
      );
    }
    return list;
  }, [orders, filter, search]);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <Reveal>
        <div className="flex-between">
          <div>
            <h2>{t('customer.ordersTitle')}</h2>
            <p>{t('customer.ordersDesc')}</p>
          </div>
          <Link to="/customer/new" className="btn btn-primary">
            {t('customer.newOrderBtn')} <IconArrowRight width={16} height={16} />
          </Link>
        </div>
      </Reveal>

      <Reveal delay={60}>
        <div className="order-filters mt-16">
          <div className="search-input-wrap">
            <input
              className="input"
              style={{ paddingLeft: 36 }}
              placeholder={t('customer.searchOrdersPlaceholder')}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <span style={{ position: 'absolute', left: 10, top: 10, color: '#9ca3af' }}>
              <IconSearch width={16} height={16} />
            </span>
          </div>
          {FILTER_KEYS.map((f) => (
            <button
              key={f}
              type="button"
              className={`filter-chip ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
            >
              {f === 'ALL' ? t('customer.filterAll') : t(`status.${f}`)}
            </button>
          ))}
        </div>
      </Reveal>

      {filtered.length === 0 ? (
        <Reveal delay={100}>
          <div className="card empty-state">
            <div className="empty-state-icon"><IconPackage width={72} height={72} /></div>
            <h3>{orders.length === 0 ? t('customer.noOrdersTitle') : t('customer.noMatchingOrdersTitle')}</h3>
            <p className="text-muted">
              {orders.length === 0
                ? t('customer.noOrdersDesc')
                : t('customer.noMatchingOrdersDesc')}
            </p>
            {orders.length === 0 && <Link to="/customer/new" className="btn btn-primary mt-16">{t('customer.placeOrderBtn')}</Link>}
          </div>
        </Reveal>
      ) : (
        filtered.map((o, i) => (
          <Reveal key={o.id} delay={Math.min(i, 6) * 50}>
            <div className="order-card hover-lift">
              <div className="order-card-head">
                <div>
                  <div className="order-track-num">{o.trackingNumber}</div>
                  <p className="order-desc">{o.description}</p>
                  <div className="order-meta">
                    <span><IconMapPin width={13} height={13} /> {o.destinationAddress}</span>
                    {o.weight && <span>{o.weight} kg</span>}
                    {o.volume && <span>{o.volume} m³</span>}
                    {o.cargoType && <span className={`cargo-type-chip cargo-type-${o.cargoType.toLowerCase()}`}>{o.cargoType}</span>}
                    {o.urgency && <span className={`cargo-type-chip urgency-${o.urgency.toLowerCase()}`}>{o.urgency}</span>}
                  </div>
                </div>
                <span className={`badge ${STATUS_CLASS[o.status]}`}>{t(`status.${o.status}`)}</span>
              </div>
              <OrderTimeline status={o.status} cancelReason={o.cancelReason} />
              <Link
                to={`/customer/track/${o.trackingNumber}`}
                className="mt-3 inline-flex items-center gap-1.5 text-xs font-semibold text-accent-blue hover:underline"
              >
                <Radio size={13} /> {o.status === 'DELIVERED' ? t('customer.viewDetailsBtn') : t('tracking.liveTrack')}
              </Link>
              <OrderPayment
                cargo={o}
                onPaid={() => setOrders((prev) => prev.map((x) => (x.id === o.id ? { ...x, paid: true } : x)))}
              />
              <div className="mt-3" style={{ borderTop: '1px dashed #e5e7eb', paddingTop: 10 }}>
                <Link to={`/customer/chat?order=${o.id}`} className="btn btn-sm flex items-center gap-1.5" style={{ width: 'fit-content' }}>
                  <MessageCircle size={13} /> {t('customer.chatWithDriverDispatcher')}
                </Link>
              </div>
              {o.status === 'DELIVERED' && o.trip?.id && <TripRating tripId={o.trip.id} />}
            </div>
          </Reveal>
        ))
      )}
    </div>
  );
}
