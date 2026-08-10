import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { X, Printer } from 'lucide-react';
import axiosClient from '../api/axiosClient';

const STATUS_KEY = {
  SUCCEEDED: { key: 'statusSucceeded', className: 'badge-success' },
  PENDING: { key: 'statusPending', className: 'badge-warning' },
  FAILED: { key: 'statusFailed', className: 'badge-danger' },
};

function formatDate(iso, locale) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' });
}

// Faktura görünüşü — müştəri, dispetçer və admin panellərinin HAMISINDA
// eyni komponentdən istifadə olunur (bax CustomerPaymentController,
// DispatcherPaymentController, AdminPaymentController — üçü də eyni
// InvoiceDetail formasını qaytarır), yalnız `apiUrl` fərqlənir. Qanunən
// fakturaya çıxışı olmalı hər iki tərəf (alıcı: müştəri; satıcının
// nümayəndələri: dispetçer/admin) buradan baxa bilir. "Çap et" düyməsi
// brauzerin öz print-to-PDF funksiyasını işə salır — ayrıca PDF kitabxanası
// tələb etmir.
export default function InvoiceView({ apiUrl, onClose }) {
  const { t, i18n } = useTranslation();
  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    axiosClient.get(apiUrl)
      .then((res) => setInvoice(res.data))
      .catch((err) => setError(err.response?.data?.message || t('invoice.errLoad')))
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
          #invoice-print-area, #invoice-print-area * { visibility: visible; }
          #invoice-print-area { position: absolute; top: 0; left: 0; width: 100%; }
          .no-print { position: static !important; background: none !important; padding: 0 !important; }
        }
      `}</style>
      <div
        className="card"
        style={{ maxWidth: 560, width: '100%', maxHeight: '86vh', overflowY: 'auto' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex-between no-print">
          <h3 style={{ margin: 0 }}>{t('invoice.title')}</h3>
          <div className="flex items-center gap-1.5">
            {invoice && (
              <button type="button" className="btn btn-sm flex items-center gap-1.5" onClick={() => window.print()}>
                <Printer size={13} /> {t('invoice.printBtn')}
              </button>
            )}
            <button type="button" className="btn btn-sm" onClick={onClose} style={{ padding: 6 }}>
              <X size={15} />
            </button>
          </div>
        </div>

        {loading && <p className="mt-16 text-muted">{t('common.loading')}</p>}
        {error && <p className="mt-16" style={{ color: 'var(--danger)' }}>{error}</p>}

        {invoice && (
          <div id="invoice-print-area" className="mt-16" style={{ fontSize: 13 }}>
            <div className="flex-between" style={{ alignItems: 'flex-start', borderBottom: '2px solid var(--border)', paddingBottom: 12 }}>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--primary)' }}>Fleetra</div>
                <div className="text-muted" style={{ fontSize: 11.5 }}>{t('invoice.companyTagline')}</div>
                <div className="text-muted" style={{ fontSize: 11.5 }}>{t('invoice.companyLocation')}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontWeight: 700 }}>{invoice.invoiceNumber}</div>
                <div className="text-muted" style={{ fontSize: 11.5 }}>{t('invoice.dateLabel')}: {formatDate(invoice.createdAt, i18n.language)}</div>
                <span className={`badge ${STATUS_KEY[invoice.status]?.className}`} style={{ marginTop: 4, display: 'inline-block' }}>
                  {STATUS_KEY[invoice.status] ? t(`dispatcher.${STATUS_KEY[invoice.status].key}`) : invoice.status}
                </span>
              </div>
            </div>

            <div className="grid grid-2 mt-16" style={{ gap: 16 }}>
              <div>
                <div className="text-muted" style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>{t('invoice.buyerLabel')}</div>
                <div style={{ fontWeight: 600 }}>{invoice.customerName || '—'}</div>
                {invoice.customerCompany && <div>{invoice.customerCompany}</div>}
                {invoice.customerPhone && <div className="text-muted">{invoice.customerPhone}</div>}
                {invoice.customerEmail && <div className="text-muted">{invoice.customerEmail}</div>}
              </div>
              <div>
                <div className="text-muted" style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>{t('invoice.orderLabel')}</div>
                <div style={{ fontFamily: "'Courier New', monospace", fontWeight: 600 }}>{invoice.trackingNumber || '—'}</div>
                {invoice.description && <div>{invoice.description}</div>}
                {invoice.weight != null && <div className="text-muted">{t('invoice.weightLabel')}: {invoice.weight} kg</div>}
                {invoice.volume != null && <div className="text-muted">{t('invoice.volumeLabel')}: {invoice.volume} m³</div>}
              </div>
            </div>

            {(invoice.pickupAddress || invoice.destinationAddress) && (
              <div className="mt-16">
                <div className="text-muted" style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>{t('invoice.routeLabel')}</div>
                <div>{invoice.pickupAddress || '—'} → {invoice.destinationAddress || '—'}</div>
              </div>
            )}

            <div className="mt-16" style={{ border: '1px solid var(--border)', borderRadius: 8, padding: 12 }}>
              <div className="flex-between">
                <span>{t('invoice.serviceFeeLabel')}</span>
                <span>{invoice.amount?.toFixed(2)} AZN</span>
              </div>
              <div className="flex-between mt-4" style={{ fontWeight: 700, fontSize: 15, borderTop: '1px solid var(--border)', paddingTop: 8, marginTop: 8 }}>
                <span>{t('invoice.totalLabel')}</span>
                <span>{invoice.amount?.toFixed(2)} AZN</span>
              </div>
            </div>

            <div className="text-muted mt-16" style={{ fontSize: 11 }}>
              {t('invoice.paymentDateLabel')}: {invoice.paidAt ? formatDate(invoice.paidAt, i18n.language) : t('invoice.notPaidYet')}
            </div>
            <div className="text-muted mt-16" style={{ fontSize: 10.5, borderTop: '1px dashed var(--border)', paddingTop: 8 }}>
              {t('invoice.footerNote')}
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
}
