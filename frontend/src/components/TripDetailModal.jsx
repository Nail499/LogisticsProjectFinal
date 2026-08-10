import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import axiosClient from '../api/axiosClient';
import LiveTrackingPanel from './LiveTrackingPanel.jsx';

// Paylaşılan "reysin ətraflı görünüşü" pəncərəsi — sürücünün "Reys
// tarixçəsi", dispetçerin "Bütün reyslər" siyahısı və (istəyə görə) admin
// panelindən bir tracking nömrəsi üçün açılır. Artıq mövcud olan
// LiveTrackingPanel-i yenidən istifadə edir (bax LiveTrackingPanel.jsx) —
// xəritədə real yol marşrutu, qət edilmiş/qalan məsafə, mərhələ zolağı,
// sürücü/nəqliyyat vasitəsi məlumatları və yol boyu xərclər artıq o
// komponentdə var, DELIVERED statuslu reyslər üçün də sükutla işləyir (bax
// LiveTrackingPanel-dəki `delivered` budaqları). /api/tracking/{trackingNumber}
// public endpoint olduğu üçün bu pəncərə sürücü/dispetçer/müştəri
// panellərinin hamısında eyni şəkildə işləyir.
export default function TripDetailModal({ trackingNumber, onClose }) {
  const { t } = useTranslation();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!trackingNumber) return undefined;
    let cancelled = false;
    setLoading(true);
    setError('');
    setData(null);
    axiosClient.get(`/api/tracking/${trackingNumber}`)
      .then((res) => { if (!cancelled) setData(res.data); })
      .catch(() => { if (!cancelled) setError(t('tracking.errNotFound')); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [trackingNumber]);

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  if (!trackingNumber) return null;

  // document.body-ə portal olaraq render edilir — DashboardLayout-un
  // ".content" elementi index.css-də "animation: pageEnter ... both" ilə
  // qurulub, bu isə son kadrda "transform: translateY(0)" saxlayır (fill
  // mode "both"). Ancaq HƏR HANSI transform (translateY(0) belə) ekrandakı
  // ata elementə position:fixed törəmələr üçün yeni "containing block"
  // yaradır — nəticədə bu pəncərə artıq VIEWPORT-a deyil, ".content"-in öz
  // (sidebar-dan kiçilmiş, kənara sürüşmüş) qutusuna görə "fixed" olurdu:
  // arxa fon tünd örtük bütün ekranı örtmürdü, pəncərənin özü də mərkəzdə
  // görünmürdü. Portal bu ata zəncirini tamamilə bypass edir.
  return createPortal(
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 150, background: 'rgba(15,23,42,0.6)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '32px 16px',
      }}
      onClick={onClose}
    >
      {/* Pəncərə əvvəllər hündürlüyü məhdudlaşdırılmamış idi (backdrop özü
          scroll edirdi) — böyük ekranlarda LiveTrackingPanel-in daxili
          xəritəsi (min-h-[560px] sətri) viewport-dan hündür məzmun yaradanda
          Leaflet xəritəsi düzgün ölçü ala bilmirdi/kəsilirdi. İndi kartın özü
          maxHeight ilə viewport-a sığdırılır və YALNIZ məzmun sahəsi scroll
          olur (flex + minHeight:0 — Leaflet-in ölçüsü artıq sabit, kəsilmiş
          konteynerdən deyil, öz flex xanasından gəlir).*/}
      <div
        className="card"
        style={{
          maxWidth: 1080, width: '100%', maxHeight: '92vh', padding: 0, overflow: 'hidden',
          display: 'flex', flexDirection: 'column',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex-between" style={{ padding: '16px 20px', borderBottom: '1px solid #e5e7eb', flexShrink: 0 }}>
          <div>
            <h3 style={{ margin: 0 }}>{t('tracking.detailModalTitle')}</h3>
            <p className="text-muted" style={{ margin: '2px 0 0', fontFamily: "'Courier New', monospace", fontSize: 13 }}>
              {trackingNumber}
            </p>
          </div>
          <button type="button" className="btn btn-sm" onClick={onClose} style={{ padding: 6 }}>
            <X size={15} />
          </button>
        </div>

        <div style={{ padding: 20, overflowY: 'auto', minHeight: 0 }}>
          {loading && <p className="text-muted">{t('common.loading')}</p>}
          {error && <div className="alert alert-error">{error}</div>}
          {data && <LiveTrackingPanel data={data} />}
        </div>
      </div>
    </div>,
    document.body
  );
}
