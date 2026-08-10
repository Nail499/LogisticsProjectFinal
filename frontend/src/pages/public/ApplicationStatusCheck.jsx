import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Search, ArrowLeft, Clock, CheckCircle2, XCircle, Hash } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';

import bannerHero from '../../assets/logzee/banner_slider_4.jpg';
import logoFooter from '../../assets/fleetra-mark.svg';

const STATUS_KEY = {
  PENDING: { key: 'applicationStatus.statusPending', icon: Clock, color: '#fe8704', bg: '#fff5ea', border: '#fed7aa' },
  APPROVED: { key: 'admin.statusApproved', icon: CheckCircle2, color: '#16a34a', bg: '#f0fdf4', border: '#bbf7d0' },
  REJECTED: { key: 'admin.statusRejected', icon: XCircle, color: '#dc2626', bg: '#fef2f2', border: '#fecaca' },
};

export default function ApplicationStatusCheck() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const [code, setCode] = useState(searchParams.get('code') || searchParams.get('id') || '');
  const [status, setStatus] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const checkStatus = async (applicationCode) => {
    setError('');
    setStatus(null);
    setLoading(true);
    try {
      const res = await axiosClient.get(`/api/applications/status/${applicationCode.trim()}`);
      setStatus(res.data);
    } catch (err) {
      setError(t('applicationStatus.errNotFound'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const initial = searchParams.get('code') || searchParams.get('id');
    if (initial) checkStatus(initial);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (code) checkStatus(code);
  };

  const statusInfo = status ? STATUS_KEY[status.status] : null;

  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-12"
      style={{ fontFamily: "'Poppins', sans-serif" }}
    >
      <div className="absolute inset-0 bg-cover bg-center" style={{ backgroundImage: `url(${bannerHero})` }} />
      <div className="absolute inset-0 bg-black/72" />

      <Reveal className="relative w-full max-w-md">
        <div className="rounded-2xl bg-white p-8 shadow-2xl sm:p-10">
          <div className="flex items-center justify-center gap-2.5">
            <img src={logoFooter} alt="Fleetra" className="h-9 w-auto" />
            <span className="text-xl font-extrabold" style={{ color: '#111827' }}>
              Fleet<span style={{ color: '#fe8704' }}>ra</span>
            </span>
          </div>

          <h2 className="text-center font-extrabold" style={{ color: '#111827', fontSize: '1.75rem', margin: '28px 0 0' }}>
            {t('applicationStatus.title')}
          </h2>
          <p className="text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>
            {t('applicationStatus.subtitle')}
          </p>

          <form onSubmit={handleSubmit} className="mt-7 flex flex-col gap-4">
            <div>
              <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                {t('applicationStatus.codeLabel')}
              </label>
              <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                <Hash size={17} style={{ color: '#fe8704' }} />
                <input
                  className="w-full border-none bg-transparent p-0 text-sm outline-none"
                  style={{ color: '#111827' }}
                  placeholder={t('applicationStatus.codePlaceholder')}
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="mt-2 flex items-center justify-center gap-2 rounded-lg py-3.5 text-sm font-bold transition-transform hover:-translate-y-0.5 disabled:opacity-60"
              style={{ background: '#fe8704', color: '#ffffff' }}
            >
              {loading ? t('applicationStatus.searching') : (<>{t('applicationStatus.checkBtn')} <Search size={16} /></>)}
            </button>
          </form>

          {error && (
            <div className="mt-5 rounded-lg px-4 py-3 text-sm" style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}>
              {error}
            </div>
          )}

          {statusInfo && (
            <div
              className="mt-5 rounded-lg px-4 py-3.5"
              style={{ background: statusInfo.bg, border: `1px solid ${statusInfo.border}` }}
            >
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold" style={{ color: '#374151' }}>{t('applicationStatus.statusLabel')}</span>
                <span className="flex items-center gap-1.5 text-sm font-bold" style={{ color: statusInfo.color }}>
                  <statusInfo.icon size={16} /> {t(statusInfo.key)}
                </span>
              </div>
              {status.rejectionReason && (
                <p className="mt-2.5 text-sm" style={{ color: '#dc2626', margin: '10px 0 0' }}>
                  {t('applicationStatus.reasonLabel')}: {status.rejectionReason}
                </p>
              )}
            </div>
          )}

          <Link
            to="/"
            className="mt-7 flex items-center justify-center gap-1.5 text-[13px] font-semibold"
            style={{ color: '#111827', marginTop: '28px' }}
          >
            <ArrowLeft size={14} /> {t('applicationStatus.backHome')}
          </Link>
        </div>
      </Reveal>
    </div>
  );
}
