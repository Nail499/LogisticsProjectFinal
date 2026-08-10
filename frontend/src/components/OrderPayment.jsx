import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CreditCard, CheckCircle2 } from 'lucide-react';
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import axiosClient from '../api/axiosClient';
import { getStripePromise } from '../utils/stripeClient';

// Stripe test rejimi ilə real ödəniş kartı forması — bax
// CustomerPaymentController/PaymentService. Stripe AZN dəstəkləmədiyi üçün
// faktiki əməliyyat "usd" ilə eyni ədədi məbləğlə gedir (test rejimində
// real pul hərəkət etmədiyi üçün fərq etmir), amma sayt daxilində qiymət
// həmişə AZN kimi göstərilir ki, digər hissələrlə (gömrük kalkulyatoru,
// hesabatlar) uyğun olsun.
function CheckoutForm({ paymentId, onSuccess, onCancel }) {
  const { t } = useTranslation();
  const stripe = useStripe();
  const elements = useElements();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;
    setSubmitting(true);
    setError('');

    const { error: stripeError, paymentIntent } = await stripe.confirmPayment({
      elements,
      redirect: 'if_required',
    });

    if (stripeError) {
      setError(stripeError.message || t('payment.errFailed'));
      setSubmitting(false);
      return;
    }

    if (paymentIntent && paymentIntent.status === 'succeeded') {
      try {
        const res = await axiosClient.post(`/api/customer/payments/${paymentId}/confirm`);
        onSuccess(res.data);
      } catch (err) {
        setError(err.response?.data?.message || t('payment.errConfirm'));
        setSubmitting(false);
      }
    } else {
      setError(t('payment.errIncomplete'));
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-3">
      <PaymentElement />
      {error && <p style={{ color: 'var(--danger)', fontSize: 11.5, marginTop: 8 }}>{error}</p>}
      <div className="flex items-center gap-2" style={{ marginTop: 10 }}>
        <button type="submit" className="btn btn-sm btn-primary" disabled={!stripe || submitting}>
          {submitting ? t('payment.confirming') : t('payment.confirmBtn')}
        </button>
        <button type="button" className="btn btn-sm" onClick={onCancel} disabled={submitting}>
          {t('payment.cancelBtn')}
        </button>
      </div>
    </form>
  );
}

export default function OrderPayment({ cargo, onPaid }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [clientSecret, setClientSecret] = useState(null);
  const [paymentId, setPaymentId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [stripePromise] = useState(() => getStripePromise());

  if (!cargo?.price) return null;

  // Qiymət sifariş yaradılanda dərhal hesablanır, amma ödəniş seçimi yalnız
  // dispetçer yükü qəbul edib (reysə təhkim edib, status PENDING-dən
  // çıxıb) göstərilməlidir — sifariş hələ "Gözləyən yüklər" siyahısındadırsa
  // ödəniş düyməsi görünməməlidir (bax CargoStatus: PENDING -> ASSIGNED
  // dispetçerin CargoQueue-də reys yaratması ilə baş verir).
  if (cargo.status === 'PENDING' || cargo.status === 'CANCELLED') return null;

  if (cargo.paid) {
    return (
      <div className="mt-3 flex items-center gap-1.5" style={{ fontSize: 12.5, color: '#16a34a', fontWeight: 600 }}>
        <CheckCircle2 size={14} /> {t('payment.paidLabel')} — {cargo.price.toFixed(2)} AZN
      </div>
    );
  }

  const startPayment = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await axiosClient.post(`/api/customer/cargo/${cargo.id}/payment-intent`);
      setClientSecret(res.data.clientSecret);
      setPaymentId(res.data.paymentId);
      setOpen(true);
    } catch (err) {
      setError(err.response?.data?.message || t('payment.errStart'));
    } finally {
      setLoading(false);
    }
  };

  const handleSuccess = (payment) => {
    setOpen(false);
    onPaid?.(payment);
  };

  return (
    <div className="mt-3" style={{ borderTop: '1px dashed #e5e7eb', paddingTop: 10 }}>
      {!open ? (
        <div className="flex items-center gap-3" style={{ flexWrap: 'wrap' }}>
          <span style={{ fontSize: 12.5, color: '#374151' }}>
            {t('payment.amountLabel')}: <strong>{cargo.price.toFixed(2)} AZN</strong>
          </span>
          <button
            type="button"
            className="btn btn-sm btn-primary flex items-center gap-1.5"
            onClick={startPayment}
            disabled={loading}
          >
            <CreditCard size={13} /> {loading ? t('payment.preparing') : t('payment.payBtn')}
          </button>
        </div>
      ) : (
        <div>
          <p className="text-muted" style={{ fontSize: 11, marginBottom: 6 }}>
            {t('payment.testCardHint')}
          </p>
          <Elements stripe={stripePromise} options={{ clientSecret }}>
            <CheckoutForm paymentId={paymentId} onSuccess={handleSuccess} onCancel={() => setOpen(false)} />
          </Elements>
        </div>
      )}
      {error && <p style={{ color: 'var(--danger)', fontSize: 11.5, marginTop: 6 }}>{error}</p>}
    </div>
  );
}
