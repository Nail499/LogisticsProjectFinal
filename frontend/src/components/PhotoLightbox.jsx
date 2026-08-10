import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { X, ChevronLeft, ChevronRight } from 'lucide-react';

// Full-page photo viewer — click a thumbnail to open, browse with
// arrows/keyboard, close with the X, Esc, or clicking the backdrop. Used
// both by the driver (previewing their own vehicle photos) and the
// customer/public tracking page (viewing the assigned vehicle's photos),
// so it takes plain resolved image URLs and knows nothing about who
// uploaded them.
export default function PhotoLightbox({ photos, initialIndex = 0, onClose }) {
  const { t } = useTranslation();
  const [index, setIndex] = useState(initialIndex);
  const count = photos?.length || 0;

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowRight') setIndex((i) => (i + 1) % count);
      if (e.key === 'ArrowLeft') setIndex((i) => (i - 1 + count) % count);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [count, onClose]);

  if (!count) return null;

  // document.body-ə portal — DashboardLayout-un ".content" elementi
  // "animation: pageEnter ... both" ilə (index.css) son kadrda
  // transform:translateY(0) saxlayır, bu isə position:fixed törəmələr üçün
  // yeni containing block yaradır (viewport əvəzinə ".content"-in öz kiçik
  // qutusuna görə "fixed" olur — tünd fon bütün ekranı örtmür). Portal bunu
  // bypass edir (bax TripDetailModal.jsx-də eyni izah).
  return createPortal(
    <div
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        // Leaflet xəritəsinin öz daxili elementləri (zoom düymələri,
        // attribution və s.) 1000-ə qədər z-index istifadə edir — 200 kifayət
        // etmirdi və xəritə lightbox-un üstündən görünürdü (bax tracking
        // səhifəsindəki "Nəqliyyat vasitəsinə bax" bug-ı). Leaflet-dən aydın
        // şəkildə yuxarıda qalması üçün 9999-a qaldırıldı.
        zIndex: 9999,
        background: 'rgba(10,10,12,0.92)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
      }}
    >
      <button
        type="button"
        onClick={onClose}
        aria-label={t('common.close')}
        style={{
          position: 'absolute', top: 18, right: 18, width: 40, height: 40, borderRadius: '50%',
          background: 'rgba(255,255,255,0.12)', border: 'none', color: '#fff', display: 'flex',
          alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }}
      >
        <X size={20} />
      </button>

      {count > 1 && (
        <div style={{ position: 'absolute', top: 18, left: 18, color: 'rgba(255,255,255,0.75)', fontSize: 13, fontWeight: 600 }}>
          {index + 1} / {count}
        </div>
      )}

      <div
        onClick={(e) => e.stopPropagation()}
        style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center', maxWidth: '90vw', maxHeight: '76vh', width: '100%' }}
      >
        {count > 1 && (
          <button
            type="button"
            onClick={() => setIndex((i) => (i - 1 + count) % count)}
            aria-label={t('common.previous')}
            style={{
              position: 'absolute', left: -8, width: 42, height: 42, borderRadius: '50%',
              background: 'rgba(255,255,255,0.12)', border: 'none', color: '#fff', display: 'flex',
              alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0,
            }}
          >
            <ChevronLeft size={22} />
          </button>
        )}

        <img
          src={photos[index]}
          alt={t('common.photoAlt', { count: index + 1 })}
          style={{ maxWidth: '100%', maxHeight: '76vh', borderRadius: 10, boxShadow: '0 20px 60px rgba(0,0,0,0.5)', objectFit: 'contain' }}
        />

        {count > 1 && (
          <button
            type="button"
            onClick={() => setIndex((i) => (i + 1) % count)}
            aria-label={t('common.next')}
            style={{
              position: 'absolute', right: -8, width: 42, height: 42, borderRadius: '50%',
              background: 'rgba(255,255,255,0.12)', border: 'none', color: '#fff', display: 'flex',
              alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0,
            }}
          >
            <ChevronRight size={22} />
          </button>
        )}
      </div>

      {count > 1 && (
        <div onClick={(e) => e.stopPropagation()} style={{ display: 'flex', gap: 8, marginTop: 18, flexWrap: 'wrap', justifyContent: 'center' }}>
          {photos.map((src, i) => (
            <button
              key={src + i}
              type="button"
              onClick={() => setIndex(i)}
              style={{
                width: 54, height: 54, borderRadius: 8, overflow: 'hidden', padding: 0, cursor: 'pointer',
                border: i === index ? '2px solid #fe8704' : '2px solid rgba(255,255,255,0.25)',
                opacity: i === index ? 1 : 0.6,
              }}
            >
              <img src={src} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </button>
          ))}
        </div>
      )}
    </div>,
    document.body
  );
}
