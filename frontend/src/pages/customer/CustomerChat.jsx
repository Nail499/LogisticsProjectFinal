import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MessageCircle, Package, Truck, Headset } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';
import OrderChat from '../../components/OrderChat.jsx';
import { STATUS_CLASS } from '../../components/OrderTimeline.jsx';

// Xüsusi "Yazışma" bölməsi — müştəri əvvəlcə yuxarıdan hansı sifarişlə bağlı
// yazışacağını seçir, sonra isə AŞAĞIDA sürücü ilə (CUSTOMER_DRIVER) və ya
// dispetçer ilə (CUSTOMER_DISPATCHER) söhbət arasında seçim edir — bax
// entity/ChatChannel. Bu iki otaq bir-birindən tam ayrıdır, sürücü dispetçer
// otağını, dispetçer/admin isə hər ikisini görə bilir (bax
// ChatService#requireAccess). MyOrders.jsx-dəki hər kartın öz kiçik "Yazış"
// düyməsi ilə yanaşı, bu səhifə bütün yazışmalar üçün mərkəzi yer rolunu
// oynayır — "?order=<cargoId>" ilə birbaşa müəyyən sifarişə keçid vermək olar.
export default function CustomerChat() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [activeChannel, setActiveChannel] = useState('CUSTOMER_DRIVER');

  useEffect(() => {
    axiosClient.get('/api/customer/cargo')
      .then((res) => {
        const list = [...res.data].sort((a, b) => (b.id || 0) - (a.id || 0));
        setOrders(list);
        const preselect = searchParams.get('order');
        if (preselect && list.some((o) => String(o.id) === preselect)) {
          setSelectedId(Number(preselect));
        } else if (list.length > 0) {
          setSelectedId(list[0].id);
        }
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selected = useMemo(() => orders.find((o) => o.id === selectedId) || null, [orders, selectedId]);

  const handleSelect = (id) => {
    setSelectedId(id);
    setSearchParams({ order: String(id) });
  };

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <Reveal>
        <h2>{t('customer.chatTitle')}</h2>
        <p>{t('customer.chatDesc')}</p>
      </Reveal>

      {orders.length === 0 ? (
        <Reveal delay={60}>
          <div className="card empty-state mt-16">
            <div className="empty-state-icon"><Package width={64} height={64} /></div>
            <h3>{t('customer.noOrdersTitle')}</h3>
            <p className="text-muted">{t('customer.chatNoOrdersDesc')}</p>
          </div>
        </Reveal>
      ) : (
        <>
          <Reveal delay={60}>
            <div className="mt-16" style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
              {orders.map((o) => (
                <button
                  key={o.id}
                  type="button"
                  onClick={() => handleSelect(o.id)}
                  className="card hover-lift"
                  style={{
                    flexShrink: 0,
                    minWidth: 220,
                    textAlign: 'left',
                    cursor: 'pointer',
                    padding: 12,
                    border: selectedId === o.id ? '2px solid var(--primary)' : '1px solid var(--border)',
                    background: selectedId === o.id ? 'var(--primary-bg)' : 'var(--surface)',
                  }}
                >
                  <div className="flex-between" style={{ alignItems: 'flex-start' }}>
                    <span style={{ fontFamily: 'monospace', fontSize: 12.5, fontWeight: 700 }}>{o.trackingNumber}</span>
                    <span className={`badge ${STATUS_CLASS[o.status]}`} style={{ fontSize: 10 }}>
                      {t(`status.${o.status}`)}
                    </span>
                  </div>
                  <div className="text-muted mt-4" style={{ fontSize: 12, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {o.description || t('customer.noDescription')}
                  </div>
                </button>
              ))}
            </div>
          </Reveal>

          {selected && (
            <Reveal delay={100}>
              <div className="card mt-16" style={{ padding: 16 }}>
                <div className="flex items-center gap-1.5" style={{ marginBottom: 12 }}>
                  <MessageCircle size={15} style={{ color: 'var(--primary)' }} />
                  <strong style={{ fontSize: 13.5 }}>{selected.trackingNumber}</strong>
                  <span className="text-muted" style={{ fontSize: 12 }}>— {selected.description || t('customer.noDescription')}</span>
                </div>

                <div style={{ display: 'flex', gap: 6, marginBottom: 12 }}>
                  <button
                    type="button"
                    onClick={() => setActiveChannel('CUSTOMER_DRIVER')}
                    className={`btn btn-sm ${activeChannel === 'CUSTOMER_DRIVER' ? 'btn-primary' : ''}`}
                    style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                  >
                    <Truck size={13} /> {t('customer.chatTabDriver')}
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveChannel('CUSTOMER_DISPATCHER')}
                    className={`btn btn-sm ${activeChannel === 'CUSTOMER_DISPATCHER' ? 'btn-primary' : ''}`}
                    style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                  >
                    <Headset size={13} /> {t('customer.chatTabDispatcher')}
                  </button>
                </div>

                <OrderChat key={`${selected.id}-${activeChannel}`} cargoId={selected.id} channel={activeChannel} />
              </div>
            </Reveal>
          )}
        </>
      )}
    </div>
  );
}
