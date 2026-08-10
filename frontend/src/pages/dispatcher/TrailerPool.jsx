import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Search, Container, User } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('az-AZ', { day: 'numeric', month: 'short' }) +
    ', ' + d.toLocaleTimeString('az-AZ', { hour: '2-digit', minute: '2-digit' });
}

// Qoşqu hovuzu (Trailer Pool) — real TMS platformalarında (McLeod və s.)
// "drop-and-hook" idarəetməsinin nüvəsi: hansı qoşqu haradadır, bazadadır/
// boşdur, yoxsa hansısa reysə bağlıdır (bağlıdırsa yüklü/boş). Əvvəllər
// dispetçer qoşqunun HAZIRKI vəziyyətini heç bir yerdə görə bilmirdi — bax
// DispatcherController#trailerPool, TrailerPoolResponse.
export default function TrailerPool() {
  const { t } = useTranslation();
  const [trailers, setTrailers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  const load = () => {
    setLoading(true);
    axiosClient.get('/api/dispatcher/trailers/pool')
      .then((res) => setTrailers(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const filtered = useMemo(() => {
    if (!search.trim()) return trailers;
    const q = search.trim().toLowerCase();
    return trailers.filter((tr) =>
      tr.plateNumber?.toLowerCase().includes(q) ||
      tr.driverName?.toLowerCase().includes(q) ||
      tr.vehiclePlate?.toLowerCase().includes(q)
    );
  }, [trailers, search]);

  const availableCount = useMemo(() => trailers.filter((tr) => !tr.onTrip).length, [trailers]);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <h2>{t('dispatcher.trailerPoolTitle')}</h2>
      <p>{t('dispatcher.trailerPoolDesc')}</p>

      <div className="card mt-16" style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
        <Container size={18} color="#fe8704" />
        <span style={{ fontSize: 13.5 }}>
          {t('dispatcher.trailerPoolAvailableCount', { count: availableCount, total: trailers.length })}
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
              <th>{t('admin.plateLabel')}</th>
              <th>{t('admin.capacityTonsLabel')}</th>
              <th>{t('admin.colOwnership')}</th>
              <th>{t('dispatcher.colStatus')}</th>
              <th>{t('dispatcher.trailerPoolColTrip')}</th>
              <th>{t('dispatcher.trailerPoolColLastGps')}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((tr) => (
              <tr key={tr.id}>
                <td>{tr.plateNumber}</td>
                <td>{tr.capacity != null ? `${tr.capacity} t` : '—'}</td>
                <td>
                  {tr.ownerType === 'DRIVER_OWNED' ? (
                    <span className="badge badge-info">{tr.ownerDriverName || t('admin.ownerDriverBadge')}</span>
                  ) : (
                    <span className="badge badge-neutral">{t('dispatcher.ownerCompanySuffix')}</span>
                  )}
                </td>
                <td>
                  {!tr.onTrip ? (
                    <span className="badge badge-neutral">{t('dispatcher.trailerPoolStatusAvailable')}</span>
                  ) : tr.loaded ? (
                    <span className="badge badge-success">{t('dispatcher.trailerPoolStatusLoaded')}</span>
                  ) : (
                    <span className="badge badge-warning">{t('dispatcher.trailerPoolStatusEmptyEnRoute')}</span>
                  )}
                </td>
                <td style={{ fontSize: 12.5 }}>
                  {tr.onTrip ? (
                    <div className="flex items-center gap-1.5">
                      <User size={12} />
                      <span>{tr.driverName || '—'}{tr.vehiclePlate ? ` (${tr.vehiclePlate})` : ''}</span>
                    </div>
                  ) : (
                    <span className="text-muted">{t('dispatcher.trailerPoolNoTrip')}</span>
                  )}
                </td>
                <td style={{ fontSize: 12.5 }} className="text-muted">
                  {tr.onTrip && tr.lastUpdatedAt ? formatDate(tr.lastUpdatedAt) : '—'}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={6} className="text-center text-muted">{t('admin.noTrailers')}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
