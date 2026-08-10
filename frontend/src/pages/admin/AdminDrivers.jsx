import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { Star, ArrowUpDown, X } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import RatingsList from '../../components/RatingsList.jsx';

const STATUS_KEY = {
  ACTIVE: { key: 'statusActive', className: 'badge-success' },
  INACTIVE: { key: 'statusInactive', className: 'badge-danger' },
};

export default function AdminDrivers() {
  const { t } = useTranslation();
  const [drivers, setDrivers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  // Performans sıralaması — admin ən yaxşı/pis reytinqli sürücüləri tez
  // görmək istəyəndə (bax bu sessiyanın "sürücü performans reytinqi"
  // istəyi). null = backend-in gətirdiyi sıra (driverId-yə görə).
  const [sortByRating, setSortByRating] = useState(null);
  // "Ətraflı bax" — bir sürücünün bütün qiymətləndirmələrini (hansı reysdən,
  // nə şərh) ayrıca modalda göstərir (bax RatingsList/AdminManagementController#driverRatings).
  const [ratingsDriver, setRatingsDriver] = useState(null);

  const load = () => {
    setLoading(true);
    axiosClient.get('/api/admin/drivers')
      .then((res) => setDrivers(res.data))
      .catch(() => setError(t('admin.errLoadDrivers')))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handleResetPassword = async (driver) => {
    const newPassword = window.prompt(t('admin.passwordPrompt', { name: driver.fullName, username: driver.username }));
    if (!newPassword) return;
    if (newPassword.length < 4) {
      alert(t('admin.passwordTooShort'));
      return;
    }
    try {
      await axiosClient.put(`/api/admin/drivers/${driver.driverId}/password`, { newPassword });
      setNotice(t('admin.passwordUpdatedNotice', { name: driver.fullName, password: newPassword }));
      setTimeout(() => setNotice(''), 8000);
    } catch (err) {
      alert(err.response?.data?.message || t('admin.errPasswordUpdate'));
    }
  };

  const handleDelete = async (driver) => {
    if (!window.confirm(t('admin.driverDeleteConfirm', { name: driver.fullName }))) return;
    try {
      await axiosClient.delete(`/api/admin/drivers/${driver.driverId}`);
      load();
    } catch (err) {
      alert(err.response?.data?.message || t('admin.errDeleteDriver'));
    }
  };

  const sortedDrivers = useMemo(() => {
    if (!sortByRating) return drivers;
    const dir = sortByRating === 'desc' ? -1 : 1;
    return [...drivers].sort((a, b) => dir * ((a.averageRating || 0) - (b.averageRating || 0)));
  }, [drivers, sortByRating]);

  const toggleSort = () => {
    setSortByRating((prev) => (prev === 'desc' ? 'asc' : prev === 'asc' ? null : 'desc'));
  };

  if (loading) return <p>{t('common.loading')}</p>;
  if (error) return <div className="alert alert-error">{error}</div>;

  return (
    <div>
      <h2>{t('admin.driversTitle')}</h2>
      <p>{t('admin.driversDesc')}</p>

      {notice && (
        <div className="alert alert-success mt-16">
          {notice}
          <button className="btn btn-sm" style={{ marginLeft: 12 }} onClick={() => setNotice('')}>{t('common.close')}</button>
        </div>
      )}

      <div className="table-wrap mt-16">
        <table>
          <thead>
            <tr>
              <th>{t('admin.colId')}</th>
              <th>{t('admin.colFullName')}</th>
              <th>{t('admin.colPhone')}</th>
              <th>{t('admin.usernameLabel')}</th>
              <th>{t('admin.colLicense')}</th>
              <th>{t('dispatcher.colStatus')}</th>
              <th>
                <button
                  type="button"
                  onClick={toggleSort}
                  className="flex items-center gap-1"
                  style={{ background: 'none', border: 'none', cursor: 'pointer', font: 'inherit', color: 'inherit', padding: 0 }}
                  title={t('admin.sortByRatingTitle')}
                >
                  {t('admin.colRating')} <ArrowUpDown size={12} />
                </button>
              </th>
              <th>{t('admin.colTrips')}</th>
              <th>{t('admin.colAction')}</th>
            </tr>
          </thead>
          <tbody>
            {sortedDrivers.map((d) => (
              <tr key={d.driverId}>
                <td>{d.driverId}</td>
                <td>{d.fullName}</td>
                <td>{d.phone}</td>
                <td>{d.username || <span className="text-muted">—</span>}</td>
                <td>{d.licenseNumber || <span className="text-muted">—</span>}</td>
                <td>
                  <span className={`badge ${STATUS_KEY[d.status]?.className}`}>
                    {STATUS_KEY[d.status] ? t(`admin.${STATUS_KEY[d.status].key}`) : d.status}
                  </span>
                </td>
                <td>
                  {d.ratingCount > 0 ? (
                    <button
                      type="button"
                      onClick={() => setRatingsDriver(d)}
                      className="flex items-center gap-1"
                      style={{ fontSize: 12.5, background: 'none', border: 'none', cursor: 'pointer', font: 'inherit', color: 'inherit', padding: 0 }}
                      title={t('driver.detailBtn')}
                    >
                      <Star size={13} fill="#fe8704" color="#fe8704" /> {d.averageRating.toFixed(1)}
                      <span className="text-muted" style={{ textDecoration: 'underline' }}>({d.ratingCount})</span>
                    </button>
                  ) : (
                    <span className="text-muted" style={{ fontSize: 12 }}>{t('admin.noRating')}</span>
                  )}
                </td>
                <td style={{ fontSize: 12.5 }}>{t('admin.tripsDeliveredCount', { count: d.deliveredTripsCount ?? 0 })}</td>
                <td>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={() => handleResetPassword(d)}
                      disabled={!d.username}
                    >
                      {t('admin.resetPasswordBtn')}
                    </button>
                    <button className="btn btn-sm btn-danger" onClick={() => handleDelete(d)}>
                      {t('common.delete')}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {drivers.length === 0 && (
              <tr><td colSpan={9} className="text-center text-muted">{t('admin.noDrivers')}</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {ratingsDriver && createPortal(
        <div
          style={{
            position: 'fixed', inset: 0, zIndex: 100, background: 'rgba(15,23,42,0.55)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16,
          }}
          onClick={() => setRatingsDriver(null)}
        >
          <div className="card" style={{ maxWidth: 640, width: '100%', maxHeight: '85vh', overflowY: 'auto' }} onClick={(e) => e.stopPropagation()}>
            <div className="flex-between">
              <h3 style={{ margin: 0 }}>{t('admin.ratingsModalTitle', { name: ratingsDriver.fullName })}</h3>
              <button type="button" className="btn btn-sm" onClick={() => setRatingsDriver(null)} style={{ padding: 6 }}>
                <X size={15} />
              </button>
            </div>
            <RatingsList apiUrl={`/api/admin/ratings/driver/${ratingsDriver.driverId}`} showDriverColumn={false} />
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
