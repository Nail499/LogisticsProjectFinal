import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Star, MessageSquare } from 'lucide-react';
import axiosClient from '../api/axiosClient';

// Müştərinin çatdırılmış reysi qiymətləndirməsi — MyOrders.jsx-də hər
// DELIVERED sifariş kartının altında görünür. Mövcud qiymətin olub-
// olmadığını GET /api/customer/trips/{tripId}/rating ilə yoxlayır (204 =
// hələ qiymətləndirilməyib), varsa read-only kimi göstərir, yoxdursa
// ulduz seçici + şərh forması açır (bax RatingService — bir reys bir
// müştəri tərəfindən yalnız bir dəfə qiymətləndirilə bilər).
export default function TripRating({ tripId }) {
  const { t } = useTranslation();
  const [existing, setExisting] = useState(undefined); // undefined = yüklənir
  const [hoverStars, setHoverStars] = useState(0);
  const [stars, setStars] = useState(0);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!tripId) return;
    axiosClient.get(`/api/customer/trips/${tripId}/rating`)
      .then((res) => setExisting(res.status === 204 ? null : res.data))
      .catch(() => setExisting(null));
  }, [tripId]);

  if (!tripId || existing === undefined) return null;

  if (existing) {
    return (
      <div className="mt-3 flex items-center gap-2" style={{ fontSize: 12.5, color: '#374151' }}>
        <span style={{ display: 'flex', gap: 1 }}>
          {[1, 2, 3, 4, 5].map((n) => (
            <Star key={n} size={13} fill={n <= existing.stars ? '#fe8704' : 'none'} color="#fe8704" />
          ))}
        </span>
        <span className="text-muted">{t('ratings.alreadyRatedLabel')}{existing.comment ? `: "${existing.comment}"` : ''}</span>
      </div>
    );
  }

  const handleSubmit = async () => {
    if (stars < 1) return;
    setSubmitting(true);
    setError('');
    try {
      const res = await axiosClient.post(`/api/customer/trips/${tripId}/rating`, { stars, comment });
      setExisting(res.data);
    } catch (err) {
      setError(err.response?.data?.message || t('ratings.errSubmit'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mt-3" style={{ borderTop: '1px dashed #e5e7eb', paddingTop: 10 }}>
      <div className="flex items-center gap-1.5" style={{ fontSize: 12, fontWeight: 600, color: '#374151', marginBottom: 6 }}>
        {t('ratings.rateTripLabel')}
      </div>
      <div className="flex items-center gap-3" style={{ flexWrap: 'wrap' }}>
        <span style={{ display: 'flex', gap: 2 }} onMouseLeave={() => setHoverStars(0)}>
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              type="button"
              onClick={() => setStars(n)}
              onMouseEnter={() => setHoverStars(n)}
              style={{ background: 'none', border: 'none', padding: 2, cursor: 'pointer' }}
              aria-label={t('ratings.starAriaLabel', { count: n })}
            >
              <Star size={20} fill={n <= (hoverStars || stars) ? '#fe8704' : 'none'} color="#fe8704" />
            </button>
          ))}
        </span>
        <div className="flex items-center gap-1.5" style={{ flex: 1, minWidth: 180 }}>
          <MessageSquare size={13} style={{ color: '#9ca3af', flexShrink: 0 }} />
          <input
            className="input"
            style={{ fontSize: 12.5, padding: '6px 8px' }}
            placeholder={t('ratings.commentPlaceholder')}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={500}
          />
        </div>
        <button
          type="button"
          className="btn btn-sm btn-primary"
          disabled={stars < 1 || submitting}
          onClick={handleSubmit}
        >
          {submitting ? t('ratings.submitting') : t('common.submit')}
        </button>
      </div>
      {error && <p style={{ color: 'var(--danger)', fontSize: 11.5, marginTop: 4 }}>{error}</p>}
    </div>
  );
}
