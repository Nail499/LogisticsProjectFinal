// Shared "who is this load for" popup — used everywhere in the dispatcher
// panel where a customer name appears (Gözləyən yüklər, Bütün reyslər,
// Control Tower map, geri dönüş/backhaul matcher), not just the pending
// queue. Accepts a LIST because one trip can carry several combined cargos
// (see CargoQueue "bir neçə yükü eyni reysə birləşdirə bilərsiniz"), each
// potentially a different customer.
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createPortal } from 'react-dom';
import { User, Phone, Mail, Building2, MapPin, X, MessageCircle, Wallet, CheckCircle2 } from 'lucide-react';
import OrderChat from '../OrderChat.jsx';
import axiosClient from '../../api/axiosClient';

// onPaid (optional): "zəngli sifariş" üçün oflayn ödəniş qeydə alınandan
// sonra çağırılır — parent siyahını (reyslər/yüklər) yenidən yükləsin deyə
// (bax DispatcherTrips.jsx/AdminTrips.jsx/BackhaulMatcher.jsx-in `load`
// funksiyaları). Verilməsə, düymə yenə işləyir, sadəcə görünüş yalnız bu
// modal bağlanana qədər lokal saxlanılır.
export default function CustomerInfoModal({ customers, onClose, onPaid }) {
  const { t } = useTranslation();
  const [openChatCargoId, setOpenChatCargoId] = useState(null);
  // Backend cavab verən kimi UI-ı dərhal "ödənilib" göstərmək üçün — parent
  // `customers` massivini asinxron yeniləyənə qədər (bax onPaid) modal köhnə
  // görüntüdə qalmasın deyə.
  const [locallyPaid, setLocallyPaid] = useState(() => new Set());
  const [payingCargoId, setPayingCargoId] = useState(null);
  const [payError, setPayError] = useState('');

  if (!customers || customers.length === 0) return null;

  const recordOfflinePayment = async (c) => {
    setPayError('');
    if (!window.confirm(t('dispatcher.recordOfflinePaymentConfirm', { amount: c.price, name: c.fullName || '—' }))) return;
    const note = window.prompt(t('dispatcher.offlinePaymentNotePrompt'), '') || '';
    setPayingCargoId(c.cargoId);
    try {
      await axiosClient.post(`/api/dispatcher/payments/cargo/${c.cargoId}/offline`, { note });
      setLocallyPaid((prev) => new Set(prev).add(c.cargoId));
      onPaid?.();
    } catch (err) {
      setPayError(err.response?.data?.message || t('dispatcher.offlinePaymentError'));
    } finally {
      setPayingCargoId(null);
    }
  };

  // Portal: bax TripDetailModal.jsx-də ".content"-in transform-animasiyasının
  // position:fixed-i necə pozduğuna dair ətraflı izah.
  return createPortal(
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 100, background: 'rgba(15,23,42,0.55)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16,
      }}
      onClick={onClose}
    >
      <div className="card" style={{ maxWidth: 440, width: '100%', maxHeight: '80vh', overflowY: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <div className="flex-between">
          <h3 style={{ margin: 0 }}>{customers.length > 1 ? t('dispatcher.customersTitle', { count: customers.length }) : t('dispatcher.customerInfoTitle')}</h3>
          <button type="button" className="btn btn-sm" onClick={onClose} style={{ padding: 6 }}>
            <X size={15} />
          </button>
        </div>

        {payError && <div className="alert alert-error mt-8" style={{ fontSize: 12.5 }}>{payError}</div>}

        <div className="mt-16" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {customers.map((c, i) => (
            <div
              key={i}
              style={{
                border: '1px solid #e5e7eb',
                borderRadius: 10,
                padding: 12,
                background: '#f9fafb',
              }}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <User size={15} style={{ color: 'var(--primary)', flexShrink: 0 }} />
                  <span style={{ fontWeight: 600 }}>{c.fullName || '—'}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Phone size={15} style={{ color: 'var(--primary)', flexShrink: 0 }} />
                  {c.phone ? <a href={`tel:${c.phone}`}>{c.phone}</a> : <span className="text-muted">—</span>}
                </div>
                {c.email && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Mail size={15} style={{ color: 'var(--primary)', flexShrink: 0 }} />
                    <a href={`mailto:${c.email}`}>{c.email}</a>
                  </div>
                )}
                {c.companyName && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Building2 size={15} style={{ color: 'var(--primary)', flexShrink: 0 }} />
                    <span>{c.companyName}</span>
                  </div>
                )}
                {c.pickupAddress && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
                    <MapPin size={14} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
                    <span className="text-muted">{c.pickupAddress}</span>
                  </div>
                )}
              </div>

              <div style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <span className={`badge ${c.registered ? 'badge-info' : 'badge-neutral'}`}>
                  {c.registered ? t('dispatcher.registeredBadge') : t('dispatcher.phoneOrderBadge')}
                </span>
                {c.trackingNumber && (
                  <span className="badge badge-neutral" style={{ fontFamily: "'Courier New', monospace" }}>
                    {c.trackingNumber}
                  </span>
                )}
                {c.cargoId && (
                  <button
                    type="button"
                    className="btn btn-sm flex items-center gap-1.5"
                    onClick={() => setOpenChatCargoId(openChatCargoId === c.cargoId ? null : c.cargoId)}
                  >
                    <MessageCircle size={12} /> {openChatCargoId === c.cargoId ? t('driver.closeChatBtn') : t('driver.chatBtn')}
                  </button>
                )}
              </div>

              {c.price != null && (() => {
                const isPaid = Boolean(c.paid) || locallyPaid.has(c.cargoId);
                return (
                  <div style={{ marginTop: 10, paddingTop: 10, borderTop: '1px dashed #e5e7eb', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' }}>
                    <div className="flex items-center gap-1.5">
                      <Wallet size={14} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
                      <span style={{ fontSize: 13 }}>{c.price} ₼</span>
                      {isPaid ? (
                        <span className="badge badge-success" style={{ fontSize: 10.5 }}>
                          <CheckCircle2 size={11} style={{ marginRight: 3 }} /> {t('dispatcher.statusSucceeded')}
                        </span>
                      ) : (
                        <span className="badge badge-warning" style={{ fontSize: 10.5 }}>{t('dispatcher.unpaidBadge')}</span>
                      )}
                    </div>
                    {!isPaid && c.cargoId && (
                      <button
                        type="button"
                        className="btn btn-sm flex items-center gap-1.5"
                        disabled={payingCargoId === c.cargoId}
                        onClick={() => recordOfflinePayment(c)}
                      >
                        <Wallet size={12} /> {payingCargoId === c.cargoId ? t('common.loading') : t('dispatcher.recordOfflinePaymentBtn')}
                      </button>
                    )}
                  </div>
                );
              })()}

              {openChatCargoId === c.cargoId && (
                <div className="mt-3">
                  <OrderChat cargoId={c.cargoId} channel="CUSTOMER_DISPATCHER" />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>,
    document.body
  );
}
