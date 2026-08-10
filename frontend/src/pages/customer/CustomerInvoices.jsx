import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Receipt, FileText } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';
import InvoiceView from '../../components/InvoiceView.jsx';

const STATUS_CLASS = {
  SUCCEEDED: 'badge-success',
  PENDING: 'badge-warning',
  FAILED: 'badge-danger',
};
const STATUS_KEY = {
  SUCCEEDED: 'statusSucceeded',
  PENDING: 'statusPending',
  FAILED: 'statusFailed',
};

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('az-AZ', { day: 'numeric', month: 'short', year: 'numeric' });
}

// Müştərinin öz ödəniş/faktura tarixçəsi (bax
// CustomerPaymentController#myPayments/invoice) — Admin/dispetçer
// "Ödənişlər" siyahısı ilə eyni InvoiceView komponentini paylaşır, sadəcə
// apiUrl fərqlənir (bax InvoiceView.jsx).
export default function CustomerInvoices() {
  const { t } = useTranslation();
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [invoicePaymentId, setInvoicePaymentId] = useState(null);

  useEffect(() => {
    axiosClient.get('/api/customer/payments')
      .then((res) => setPayments(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <Reveal>
        <h2>{t('customer.invoicesTitle')}</h2>
        <p>{t('customer.invoicesDesc')}</p>
      </Reveal>

      {payments.length === 0 ? (
        <Reveal delay={60}>
          <div className="card empty-state mt-16">
            <div className="empty-state-icon"><Receipt width={64} height={64} /></div>
            <h3>{t('customer.noInvoicesTitle')}</h3>
            <p className="text-muted">{t('customer.noInvoicesDesc')}</p>
          </div>
        </Reveal>
      ) : (
        <Reveal delay={60}>
          <div className="table-wrap mt-16">
            <table>
              <thead>
                <tr>
                  <th>{t('dispatcher.colTracking')}</th>
                  <th>{t('dispatcher.colAmount')}</th>
                  <th>{t('dispatcher.colStatus')}</th>
                  <th>{t('dispatcher.colCreated')}</th>
                  <th>{t('dispatcher.colPaid')}</th>
                  <th>{t('dispatcher.colInvoice')}</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td style={{ fontFamily: "'Courier New', monospace" }}>{p.trackingNumber || '—'}</td>
                    <td>{p.amount?.toFixed(2)} AZN</td>
                    <td>
                      <span className={`badge ${STATUS_CLASS[p.status]}`}>
                        {STATUS_KEY[p.status] ? t(`dispatcher.${STATUS_KEY[p.status]}`) : p.status}
                      </span>
                    </td>
                    <td style={{ fontSize: 12.5 }}>{formatDate(p.createdAt)}</td>
                    <td style={{ fontSize: 12.5 }}>{formatDate(p.paidAt)}</td>
                    <td>
                      <button type="button" className="btn btn-sm flex items-center gap-1.5" onClick={() => setInvoicePaymentId(p.id)}>
                        <FileText size={12} /> {t('dispatcher.viewBtn')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Reveal>
      )}

      {invoicePaymentId && (
        <InvoiceView apiUrl={`/api/customer/payments/${invoicePaymentId}/invoice`} onClose={() => setInvoicePaymentId(null)} />
      )}
    </div>
  );
}
