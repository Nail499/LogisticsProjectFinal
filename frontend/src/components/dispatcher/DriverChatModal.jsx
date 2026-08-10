// Dispetçerin sürücü ilə DAXİLİ yazışması (bax entity/ChatChannel — müştəri
// bu söhbəti heç vaxt görmür). Portal: bax CustomerInfoModal.jsx-də eyni
// izah (".content"-in transform-animasiyası position:fixed-i pozur).
import { useTranslation } from 'react-i18next';
import { createPortal } from 'react-dom';
import { X, Truck } from 'lucide-react';
import OrderChat from '../OrderChat.jsx';

export default function DriverChatModal({ trip, onClose }) {
  const { t } = useTranslation();
  if (!trip) return null;
  // Otaq açarı kimi reysin birinci yükünün cargoId-si istifadə olunur (bax
  // DriverCurrentTrip.jsx-dəki eyni qərar) — bir reys = bir daxili otaq.
  const cargoId = trip.customers?.[0]?.cargoId;
  if (!cargoId) return null;

  return createPortal(
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 100, background: 'rgba(15,23,42,0.55)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16,
      }}
      onClick={onClose}
    >
      <div className="card" style={{ maxWidth: 440, width: '100%' }} onClick={(e) => e.stopPropagation()}>
        <div className="flex-between">
          <h3 className="flex items-center gap-1.5" style={{ margin: 0, fontSize: 15 }}>
            <Truck size={16} style={{ color: 'var(--primary)' }} /> {t('dispatcher.internalChatTitle', { name: trip.driverName || t('dispatcher.driverFallback') })}
          </h3>
          <button type="button" className="btn btn-sm" onClick={onClose} style={{ padding: 6 }}>
            <X size={15} />
          </button>
        </div>
        <p className="text-muted mt-4" style={{ fontSize: 11.5 }}>{t('dispatcher.internalChatHint')}</p>
        <div className="mt-8">
          <OrderChat cargoId={cargoId} channel="INTERNAL" />
        </div>
      </div>
    </div>,
    document.body
  );
}
