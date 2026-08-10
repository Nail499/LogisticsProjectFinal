import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Search, ArrowLeft } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import LiveTrackingPanel from '../../components/LiveTrackingPanel.jsx';
import Logo from '../../components/Logo.jsx';

// Public tracking page — reached straight from the marketing Home page, with
// no dashboard shell around it. It used to sit on the generic dark blue
// ".auth-wrap"/".auth-card" (login/register) look, which visually clashed
// with the rest of the site: Home.jsx and every orange-themed dashboard
// (Admin/Dispatcher/Customer, see DashboardLayout) use white surfaces + the
// Fleetra orange (#fe8704) brand color. Fixed by giving this page the same
// plain white marketing-site shell (mini header with the real Logo, white
// background) and wrapping it in the reusable `theme-orange` class (see
// index.css) so every shared component below — .btn-primary, .input focus
// ring, and LiveTrackingPanel's var(--primary) accents — resolves to the
// same orange instead of the generic blue :root default.
export default function TrackingSearch() {
  const { t } = useTranslation();
  const [trackingNumber, setTrackingNumber] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const res = await axiosClient.get(`/api/tracking/${trackingNumber}`);
      setResult(res.data);
    } catch (err) {
      setError(t('tracking.notFound'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="theme-orange" style={{ minHeight: '100vh', background: '#fff' }}>
      <header style={{ borderBottom: '1px solid #e5e7eb', background: '#fff' }}>
        <div
          style={{
            maxWidth: 1120,
            margin: '0 auto',
            padding: '16px 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <Link to="/" style={{ display: 'flex', alignItems: 'center' }}>
            <Logo size={30} />
          </Link>
          <Link
            to="/"
            style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 14, fontWeight: 600, color: '#374151' }}
          >
            <ArrowLeft size={15} /> {t('tracking.backHome')}
          </Link>
        </div>
      </header>

      <div style={{ padding: '48px 20px' }}>
        <div style={{ maxWidth: result ? 960 : 440, width: '100%', margin: '0 auto' }}>
          <div
            style={{
              background: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: 16,
              padding: 36,
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: '50%',
                background: 'var(--primary-bg)',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto',
              }}
            >
              <Search size={22} />
            </div>
            <h2 className="text-center mt-16" style={{ color: '#111827' }}>{t('tracking.searchTitle')}</h2>
            <p className="text-center mt-8" style={{ color: '#6b7280' }}>{t('tracking.searchDesc')}</p>

            <form onSubmit={handleSubmit} className="mt-24">
              <div className="form-group">
                <input
                  className="input"
                  value={trackingNumber}
                  onChange={(e) => setTrackingNumber(e.target.value)}
                  placeholder={t('tracking.searchPlaceholder')}
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
                {loading ? t('tracking.searching') : t('tracking.searchButton')}
              </button>
            </form>

            {error && <div className="alert alert-error mt-16">{error}</div>}
          </div>

          {result && (
            <div className="mt-24">
              <LiveTrackingPanel data={result} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
