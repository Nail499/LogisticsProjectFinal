import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CreditCard, Search, FileDown, Receipt } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import { downloadCsv } from '../../utils/csvExport.js';
import InvoiceView from '../../components/InvoiceView.jsx';

const STATUS_KEY = {
  SUCCEEDED: { key: 'statusSucceeded', className: 'badge-success' },
  PENDING: { key: 'statusPending', className: 'badge-warning' },
  FAILED: { key: 'statusFailed', className: 'badge-danger' },
};

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('az-AZ', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ', ' + d.toLocaleTimeString('az-AZ', { hour: '2-digit', minute: '2-digit' });
}

// Admin "Ödənişlər" — Stripe test rejimi ilə edilmiş bütün ödənişlərin
// sadə faktura siyahısı (bax AdminPaymentController, PaymentService).
export default function AdminPayments() {
  const { t } = useTranslation();
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [invoicePaymentId, setInvoicePaymentId] = useState(null);

  useEffect(() => {
    axiosClient.get('/api/admin/payments')
      .then((res) => setPayments(res.data))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    if (!search.trim()) return payments;
    const q = search.trim().toLowerCase();
    return payments.filter((p) =>
      p.trackingNumber?.toLowerCase().includes(q) ||
      p.customerName?.toLowerCase().includes(q) ||
      p.customerEmail?.toLowerCase().includes(q)
    );
  }, [payments, search]);

  const totalReceived = useMemo(
    () => payments.filter((p) => p.status === 'SUCCEEDED').reduce((sum, p) => sum + (p.amount || 0), 0),
    [payments]
  );

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <div className="flex-between">
        <div>
          <h2>{t('dispatcher.paymentsTitle')}</h2>
          <p>{t('admin.paymentsDesc')}</p>
        </div>
        <button
          type="button"
          className="btn btn-sm flex items-center gap-1.5"
          onClick={() => downloadCsv('/api/admin/export/payments.csv', 'odenisler.csv')}
        >
          <FileDown size={14} /> {t('admin.exportBtn')}
        </button>
      </div>

      <div className="card mt-16" style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
        <CreditCard size={18} color="#fe8704" />
        <span style={{ fontSize: 13.5 }}>
          {t('dispatcher.totalReceived')}: <strong>{totalReceived.toFixed(2)} AZN</strong>
          <span className="text-muted"> {t('dispatcher.paymentsCount', { count: payments.filter((p) => p.status === 'SUCCEEDED').length })}</span>
        </span>
      </div>

      <div className="search-input-wrap mt-16" style={{ maxWidth: 360 }}>
        <input
          className="input"
          style={{ paddingLeft: 36 }}
          placeholder={t('dispatcher.searchPlaceholder')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <span style={{ position: 'absolute', left: 10, top: 10, color: '#9ca3af' }}>
          <Search width={16} height={16} />
        </span>
      </div>

      <div className="table-wrap mt-16">
        <table>
          <thead>
            <tr>
              <th>{t('dispatcher.colTracking')}</th>
              <th>{t('dispatcher.colCustomer')}</th>
              <th>{t('dispatcher.colAmount')}</th>
              <th>{t('dispatcher.colStatus')}</th>
              <th>{t('dispatcher.colMethod')}</th>
              <th>{t('dispatcher.colCreated')}</th>
              <th>{t('dispatcher.colPaid')}</th>
              <th>{t('dispatcher.colInvoice')}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((p) => (
              <tr key={p.id}>
                <td>{p.trackingNumber || <span className="text-muted">—</span>}</td>
                <td>
                  <div>{p.customerName || <span className="text-muted">—</span>}</div>
                  {p.customerEmail && <div className="text-muted" style={{ fontSize: 11.5 }}>{p.customerEmail}</div>}
                </td>
                <td>{p.amount?.toFixed(2)} AZN</td>
                <td>
                  <span className={`badge ${STATUS_KEY[p.status]?.className}`}>
                    {STATUS_KEY[p.status] ? t(`dispatcher.${STATUS_KEY[p.status].key}`) : p.status}
                  </span>
                </td>
                <td>
                  <span className={`badge ${p.method === 'OFFLINE_DISPATCHER' ? 'badge-neutral' : 'badge-info'}`} style={{ fontSize: 10.5 }} title={p.offlineNote || undefined}>
                    {p.method === 'OFFLINE_DISPATCHER' ? t('dispatcher.methodOffline') : t('dispatcher.methodStripe')}
                  </span>
                </td>
                <td style={{ fontSize: 12.5 }}>{formatDate(p.createdAt)}</td>
                <td style={{ fontSize: 12.5 }}>{formatDate(p.paidAt)}</td>
                <td>
                  <button type="button" className="btn btn-sm flex items-center gap-1.5" onClick={() => setInvoicePaymentId(p.id)}>
                    <Receipt size={12} /> {t('dispatcher.viewBtn')}
                  </button>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={8} className="text-center text-muted">{t('dispatcher.noPayments')}</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {invoicePaymentId && (
        <InvoiceView apiUrl={`/api/admin/payments/${invoicePaymentId}/invoice`} onClose={() => setInvoicePaymentId(null)} />
      )}
    </div>
  );
}
