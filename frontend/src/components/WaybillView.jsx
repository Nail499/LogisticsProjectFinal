import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { X, Printer } from 'lucide-react';
import axiosClient from '../api/axiosClient';

function formatDate(iso, locale) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' });
}

// "Yük qaiməsi" (əmtəə-nəqliyyat qaiməsi) — bax backend
// dto/CargoWaybillDetail-dəki qeyd: yolda nəzarət yoxlaması zamanı sürücüdə
// olmalı sənəd, InvoiceView-dan (kommersiya fakturası, məbləğ daşıyır)
// QƏSDƏN fərqli komponentdir — burada məbləğ/ödəniş məlumatı YOXDUR,
// yalnız yükün özü (nə, kimdən-kimə, hansı ünvanlar arası) göstərilir.
export default function WaybillView({ apiUrl, onClose }) {
  const { t, i18n } = useTranslation();
  const [waybill, setWaybill] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    axiosClient.get(apiUrl)
      .then((res) => setWaybill(res.data))
      .catch((err) => setError(err.response?.data?.message || t('waybill.errLoad')))
      .finally(() => setLoading(false));
  }, [apiUrl]);

  return createPortal(
    <div
      className="no-print"
      style={{
        position: 'fixed', inset: 0, zIndex: 200, background: 'rgba(15,23,42,0.55)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16,
      }}
      onClick={onClose}
    >
      <style>{`
        @media print {
          body * { visibility: hidden; }
          #waybill-print-area, #waybill-print-area * { visibility: visible; }
          #waybill-print-area { position: absolute; top: 0; left: 0; width: 100%; }
          .no-print { position: static !important; background: none !important; padding: 0 !important; }
        }
      `}</style>
      <div
        className="card"
        style={{ maxWidth: 560, width: '100%', maxHeight: '86vh', overflowY: 'auto' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex-between no-print">
          <h3 style={{ margin: 0 }}>{t('waybill.title')}</h3>
          <div className="flex items-center gap-1.5">
            {waybill && (
              <button type="button" className="btn btn-sm flex items-center gap-1.5" onClick={() => window.print()}>
                <Printer size={13} /> {t('waybill.printBtn')}
              </button>
            )}
            <button type="button" className="btn btn-sm" onClick={onClose} style={{ padding: 6 }}>
              <X size={15} />
            </button>
          </div>
        </div>

        {loading && <p className="mt-16 text-muted">{t('common.loading')}</p>}
        {error && <p className="mt-16" style={{ color: 'var(--danger)' }}>{error}</p>}

        {waybill && (
          <div id="waybill-print-area" className="mt-16" style={{ fontSize: 13 }}>
            <div className="flex-between" style={{ alignItems: 'flex-start', borderBottom: '2px solid var(--border)', paddingBottom: 12 }}>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--primary)' }}>Fleetra</div>
                <div className="text-muted" style={{ fontSize: 11.5 }}>{t('waybill.subtitle')}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontFamily: "'Courier New', monospace", fontWeight: 700 }}>{waybill.trackingNumber}</div>
                <div className="text-muted" style={{ fontSize: 11.5 }}>{t('waybill.dateLabel')}: {formatDate(waybill.createdAt, i18n.language)}</div>
              </div>
            </div>

            <div className="grid grid-2 mt-16" style={{ gap: 16 }}>
              <div>
                <div className="text-muted" style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>{t('waybill.senderLabel')}</div>
                <div style={{ fontWeight: 600 }}>{waybill.senderName || '—'}</div>
                <div className="text-muted">{waybill.pickupAddress || '—'}</div>
              </div>
              <div>
                <div className="text-muted" style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>{t('waybill.receiverLabel')}</div>
                <div style={{ fontWeight: 600 }}>{waybill.receiverName || '—'}</div>
                <div className="text-muted">{waybill.destinationAddress || '—'}</div>
                {waybill.receiverPhone && <div className="text-muted">{waybill.receiverPhone}</div>}
              </div>
            </div>

            <div className="mt-16">
              <div className="text-muted" style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>{t('waybill.cargoLabel')}</div>
              <div>{waybill.description || '—'}</div>
              <div className="text-muted mt-4" style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                {waybill.cargoType && <span>{t('waybill.typeLabel')}: {waybill.cargoType}</span>}
                {waybill.weight != null && <span>{t('waybill.weightLabel')}: {waybill.weight} kg</span>}
                {waybill.volume != null && <span>{t('waybill.volumeLabel')}: {waybill.volume} m³</span>}
              </div>
            </div>

            <div className="mt-16" style={{ border: '1px solid var(--border)', borderRadius: 8, padding: 12 }}>
              <div className="text-muted" style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>{t('waybill.transportLabel')}</div>
              <div>{t('waybill.driverLabel')}: {waybill.driverName || '—'}</div>
              <div>{t('waybill.vehicleLabel')}: {waybill.vehiclePlate || '—'}</div>
              {waybill.routeInfo && <div>{t('waybill.routeLabel')}: {waybill.routeInfo}</div>}
            </div>

            <div className="text-muted mt-16" style={{ fontSize: 10.5, borderTop: '1px dashed var(--border)', paddingTop: 8 }}>
              {t('waybill.footerNote')}
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
}
