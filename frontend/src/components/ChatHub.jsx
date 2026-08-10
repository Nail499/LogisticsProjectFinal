import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MessageCircle, Package } from 'lucide-react';
import axiosClient from '../api/axiosClient';
import Reveal from './Reveal.jsx';
import OrderChat from './OrderChat.jsx';
import { STATUS_CLASS } from './OrderTimeline.jsx';

// Mərkəzi "Yazışma" bölməsi — CustomerChat.jsx-dəki eyni model, sadəcə
// ümumiləşdirilib ki, dispetçer/admin/sürücü panellərində də təkrar
// istifadə oluna bilsin. Sifariş siyahısı `/api/chat/cargo-list`-dən (bax
// ChatCargoController#cargoList, ChatService#listChatCargos) rola görə
// fərqli gəlir, kanal tabları isə `tabs` prop-u ilə çağıran səhifə
// tərəfindən müəyyən olunur:
//   - Dispetçer/admin: Müştəri ilə (CUSTOMER_DISPATCHER) + Sürücü ilə (INTERNAL)
//   - Sürücü: Müştəri ilə (CUSTOMER_DRIVER) + Dispetçer ilə (INTERNAL)
export default function ChatHub({ title, subtitle, tabs, emptyText }) {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [cargos, setCargos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [activeChannel, setActiveChannel] = useState(tabs[0].key);
  const [search, setSearch] = useState('');

  useEffect(() => {
    axiosClient.get('/api/chat/cargo-list')
      .then((res) => {
        setCargos(res.data);
        const preselect = searchParams.get('order');
        if (preselect && res.data.some((c) => String(c.cargoId) === preselect)) {
          setSelectedId(Number(preselect));
        } else if (res.data.length > 0) {
          setSelectedId(res.data[0].cargoId);
        }
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtered = useMemo(() => {
    if (!search.trim()) return cargos;
    const q = search.trim().toLowerCase();
    return cargos.filter((c) =>
      c.trackingNumber?.toLowerCase().includes(q)
      || c.customerName?.toLowerCase().includes(q)
      || c.driverName?.toLowerCase().includes(q));
  }, [cargos, search]);

  const selected = useMemo(() => cargos.find((c) => c.cargoId === selectedId) || null, [cargos, selectedId]);

  const handleSelect = (id) => {
    setSelectedId(id);
    setSearchParams({ order: String(id) });
  };

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <Reveal>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </Reveal>

      {cargos.length === 0 ? (
        <Reveal delay={60}>
          <div className="card empty-state mt-16">
            <div className="empty-state-icon"><Package width={64} height={64} /></div>
            <h3>{t('customer.noOrdersTitle')}</h3>
            <p className="text-muted">{emptyText}</p>
          </div>
        </Reveal>
      ) : (
        <>
          <Reveal delay={60}>
            <input
              className="input mt-16"
              placeholder={t('common.search')}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ maxWidth: 320 }}
            />
            <div className="mt-12" style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
              {filtered.map((c) => (
                <button
                  key={c.cargoId}
                  type="button"
                  onClick={() => handleSelect(c.cargoId)}
                  className="card hover-lift"
                  style={{
                    flexShrink: 0,
                    minWidth: 220,
                    textAlign: 'left',
                    cursor: 'pointer',
                    padding: 12,
                    border: selectedId === c.cargoId ? '2px solid var(--primary)' : '1px solid var(--border)',
                    background: selectedId === c.cargoId ? 'var(--primary-bg)' : 'var(--surface)',
                  }}
                >
                  <div className="flex-between" style={{ alignItems: 'flex-start' }}>
                    <span style={{ fontFamily: 'monospace', fontSize: 12.5, fontWeight: 700 }}>{c.trackingNumber}</span>
                    <span className={`badge ${STATUS_CLASS[c.status] || 'badge-info'}`} style={{ fontSize: 10 }}>
                      {STATUS_CLASS[c.status] ? t(`status.${c.status}`) : c.status}
                    </span>
                  </div>
                  <div className="text-muted mt-4" style={{ fontSize: 12, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {c.customerName || t('customer.noCustomerFallback')}{c.driverName ? ` • ${c.driverName}` : ''}
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
                  <span className="text-muted" style={{ fontSize: 12 }}>— {selected.customerName || t('customer.noCustomerFallback')}</span>
                </div>

                {tabs.length > 1 && (
                  <div style={{ display: 'flex', gap: 6, marginBottom: 12 }}>
                    {tabs.map((tab) => (
                      <button
                        key={tab.key}
                        type="button"
                        onClick={() => setActiveChannel(tab.key)}
                        className={`btn btn-sm ${activeChannel === tab.key ? 'btn-primary' : ''}`}
                        style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                      >
                        {tab.icon}{tab.label}
                      </button>
                    ))}
                  </div>
                )}

                <OrderChat key={`${selected.cargoId}-${activeChannel}`} cargoId={selected.cargoId} channel={activeChannel} />
              </div>
            </Reveal>
          )}
        </>
      )}
    </div>
  );
}
