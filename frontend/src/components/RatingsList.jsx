import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Star, Search, MessageSquareWarning, Truck, User, MapPin } from 'lucide-react';
import axiosClient from '../api/axiosClient';

function timeAgo(iso, locale, t) {
  if (!iso) return '—';
  const diffMs = Date.now() - new Date(iso).getTime();
  const min = Math.floor(diffMs / 60000);
  if (min < 1) return t('ratings.justNow');
  if (min < 60) return t('ratings.minutesAgo', { count: min });
  const hr = Math.floor(min / 60);
  if (hr < 24) return t('ratings.hoursAgo', { count: hr });
  const day = Math.floor(hr / 24);
  if (day < 30) return t('ratings.daysAgo', { count: day });
  return new Date(iso).toLocaleDateString(locale, { day: 'numeric', month: 'short', year: 'numeric' });
}

function Stars({ value, size = 13 }) {
  return (
    <span className="flex items-center" style={{ gap: 1 }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Star key={n} size={size} fill={n <= value ? '#fe8704' : 'none'} color={n <= value ? '#fe8704' : '#d1d5db'} />
      ))}
    </span>
  );
}

// Bax RatingService#getAllRatingsDetailed / getDriverRatingsDetailed —
// admin, dispetçer və sürücü panellərində eyni "hansı reysdən nə
// qiymət/şərh alınıb" görünüşünü paylaşan ortaq komponent. showDriverColumn
// sürücünün öz "Reytinqlərim" səhifəsində false verilir (özünü göstərməyə
// ehtiyac yoxdur).
export default function RatingsList({ apiUrl, showDriverColumn = true }) {
  const { t, i18n } = useTranslation();
  const [ratings, setRatings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [starFilter, setStarFilter] = useState('ALL');

  const STAR_FILTERS = [
    { key: 'ALL', label: t('ratings.filterAll') },
    { key: 5, label: '5' },
    { key: 4, label: '4' },
    { key: 3, label: '3' },
    { key: 2, label: '2' },
    { key: 1, label: '1' },
    { key: 'LOW', label: t('ratings.filterProblematic') },
  ];

  useEffect(() => {
    setLoading(true);
    setError('');
    axiosClient.get(apiUrl)
      .then((res) => setRatings(res.data))
      .catch(() => setError(t('ratings.errLoad')))
      .finally(() => setLoading(false));
  }, [apiUrl]);

  const summary = useMemo(() => {
    const total = ratings.length;
    const distribution = [0, 0, 0, 0, 0]; // index 0 -> 1 ulduz ... index 4 -> 5 ulduz
    let sum = 0;
    ratings.forEach((r) => {
      sum += r.stars;
      if (r.stars >= 1 && r.stars <= 5) distribution[r.stars - 1] += 1;
    });
    return {
      total,
      average: total > 0 ? Math.round((sum / total) * 10) / 10 : 0,
      distribution,
    };
  }, [ratings]);

  const filtered = useMemo(() => {
    let list = ratings;
    if (starFilter === 'LOW') list = list.filter((r) => r.stars <= 2);
    else if (starFilter !== 'ALL') list = list.filter((r) => r.stars === starFilter);

    const q = search.trim().toLowerCase();
    if (q) {
      list = list.filter((r) =>
        r.driverName?.toLowerCase().includes(q) ||
        r.customerName?.toLowerCase().includes(q) ||
        r.trackingNumber?.toLowerCase().includes(q) ||
        r.comment?.toLowerCase().includes(q) ||
        r.routeInfo?.toLowerCase().includes(q)
      );
    }
    return list;
  }, [ratings, starFilter, search]);

  if (loading) return <p>{t('common.loading')}</p>;
  if (error) return <div className="alert alert-error">{error}</div>;

  return (
    <div>
      {/* Ümumi göstəricilər */}
      <div className="grid grid-2 mt-16" style={{ gap: 12 }}>
        <div className="card" style={{ padding: 16 }}>
          <div className="text-muted" style={{ fontSize: 12 }}>{t('ratings.averageRatingLabel')}</div>
          <div className="flex items-center gap-2 mt-8">
            <span style={{ fontSize: 26, fontWeight: 800 }}>{summary.total > 0 ? summary.average.toFixed(1) : '—'}</span>
            {summary.total > 0 && <Stars value={Math.round(summary.average)} size={16} />}
            <span className="text-muted" style={{ fontSize: 12 }}>{t('ratings.ratingsCountLabel', { count: summary.total })}</span>
          </div>
        </div>
        <div className="card" style={{ padding: 16 }}>
          <div className="text-muted" style={{ fontSize: 12, marginBottom: 8 }}>{t('ratings.starDistributionLabel')}</div>
          {[5, 4, 3, 2, 1].map((n) => {
            const count = summary.distribution[n - 1];
            const pct = summary.total > 0 ? (count / summary.total) * 100 : 0;
            return (
              <div key={n} className="flex items-center gap-2" style={{ marginBottom: 3 }}>
                <span style={{ fontSize: 11, width: 10 }}>{n}</span>
                <Star size={11} fill="#fe8704" color="#fe8704" />
                <div style={{ flex: 1, height: 6, borderRadius: 99, background: 'var(--border)', overflow: 'hidden' }}>
                  <div style={{ width: `${pct}%`, height: '100%', background: 'var(--primary)', borderRadius: 99 }} />
                </div>
                <span className="text-muted" style={{ fontSize: 11, width: 20, textAlign: 'right' }}>{count}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Filtrlər */}
      <div className="flex items-center gap-8 mt-16" style={{ flexWrap: 'wrap' }}>
        <div className="search-input-wrap" style={{ maxWidth: 320, position: 'relative', flex: 1 }}>
          <input
            className="input"
            style={{ paddingLeft: 36 }}
            placeholder={t('ratings.searchPlaceholder')}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <span style={{ position: 'absolute', left: 10, top: 10, color: '#9ca3af' }}>
            <Search width={16} height={16} />
          </span>
        </div>
        <div className="flex items-center gap-1" style={{ flexWrap: 'wrap' }}>
          {STAR_FILTERS.map((f) => (
            <button
              key={f.key}
              type="button"
              className={`btn btn-sm ${starFilter === f.key ? 'btn-primary' : ''}`}
              onClick={() => setStarFilter(f.key)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Siyahı */}
      <div className="mt-16" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {filtered.map((r) => (
          <div
            key={r.id}
            className="card"
            style={{
              padding: 14,
              border: r.stars <= 2 ? '1px solid var(--danger)' : '1px solid var(--border)',
              background: r.stars <= 2 ? 'var(--danger-bg)' : 'var(--surface)',
            }}
          >
            <div className="flex-between" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 8 }}>
              <div style={{ minWidth: 0 }}>
                <Stars value={r.stars} />
                <div className="mt-4" style={{ fontSize: 13.5, color: 'var(--text)' }}>
                  {r.comment ? `„${r.comment}"` : <span className="text-muted" style={{ fontStyle: 'italic' }}>{t('ratings.noCommentLabel')}</span>}
                </div>
              </div>
              <div className="text-muted" style={{ fontSize: 11, whiteSpace: 'nowrap' }}>{timeAgo(r.createdAt, i18n.language, t)}</div>
            </div>

            <div className="flex items-center gap-12 mt-8" style={{ flexWrap: 'wrap', fontSize: 12 }}>
              {showDriverColumn && (
                <span className="flex items-center gap-1.5">
                  <Truck size={12} style={{ color: 'var(--primary)' }} />
                  {r.driverName || t('ratings.unknownDriver')}{r.vehiclePlate ? ` (${r.vehiclePlate})` : ''}
                </span>
              )}
              <span className="flex items-center gap-1.5">
                <User size={12} style={{ color: 'var(--primary)' }} /> {r.customerName || t('ratings.unknownCustomer')}
              </span>
              {r.trackingNumber && (
                <span className="badge badge-neutral" style={{ fontFamily: "'Courier New', monospace" }}>{r.trackingNumber}</span>
              )}
              {r.routeInfo && (
                <span className="flex items-center gap-1.5 text-muted">
                  <MapPin size={12} /> {r.routeInfo}
                </span>
              )}
              <span className="text-muted">{t('dispatcher.tripFallback', { id: r.tripId })}</span>
              {r.stars <= 2 && (
                <span className="flex items-center gap-1" style={{ color: 'var(--danger)', fontWeight: 600 }}>
                  <MessageSquareWarning size={12} /> {t('ratings.needsAttention')}
                </span>
              )}
            </div>
          </div>
        ))}

        {filtered.length === 0 && (
          <div className="card text-center text-muted" style={{ padding: 24 }}>
            <Star size={26} style={{ opacity: 0.4, margin: '0 auto 8px', display: 'block' }} />
            {t('ratings.noResultsFound')}
          </div>
        )}
      </div>
    </div>
  );
}
