import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LogIn, Lock, User, ArrowLeft } from 'lucide-react';
import { useAuth } from '../../context/AuthContext.jsx';
import Reveal from '../../components/Reveal.jsx';
import LanguageSwitcher from '../../components/LanguageSwitcher.jsx';

import bannerHero from '../../assets/logzee/banner_slider_4.jpg';
import logoFooter from '../../assets/fleetra-mark.svg';

const ROLE_HOME = {
  ADMIN: '/admin',
  DISPATCHER: '/dispatcher',
  DRIVER: '/driver',
  CUSTOMER: '/customer',
};

export default function Login() {
  const { t } = useTranslation();
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await login(username, password);
      navigate(ROLE_HOME[user.role] || '/');
    } catch (err) {
      setError(err.response?.data?.message || t('auth.loginError'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-12"
      style={{ fontFamily: "'Poppins', sans-serif" }}
    >
      <div
        className="absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: `url(${bannerHero})` }}
      />
      <div className="absolute inset-0 bg-black/72" />

      <div className="absolute right-4 top-4 z-[60] sm:right-8 sm:top-8">
        <LanguageSwitcher variant="light" />
      </div>

      <Reveal className="relative w-full max-w-md">
        <div className="rounded-2xl bg-white p-8 shadow-2xl sm:p-10">
          <div className="flex items-center justify-center gap-2.5">
            <img src={logoFooter} alt="Fleetra" className="h-9 w-auto" />
            <span className="text-xl font-extrabold" style={{ color: '#111827' }}>
              Fleet<span style={{ color: '#fe8704' }}>ra</span>
            </span>
          </div>

          <h2
            className="mt-7 text-center font-extrabold"
            style={{ color: '#111827', fontSize: '1.75rem', margin: '28px 0 0' }}
          >
            {t('auth.loginTitle')}
          </h2>
          <p className="mt-2 text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>
            {t('auth.loginSubtitle')}
          </p>

          {error && (
            <div
              className="mt-5 rounded-lg px-4 py-3 text-sm"
              style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}
            >
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-7 flex flex-col gap-4">
            <div>
              <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                {t('auth.usernameLabel')}
              </label>
              <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                <User size={17} style={{ color: '#fe8704' }} />
                <input
                  className="w-full border-none bg-transparent p-0 text-sm outline-none"
                  style={{ color: '#111827' }}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder={t('auth.usernamePlaceholder')}
                  required
                />
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                {t('auth.passwordLabel')}
              </label>
              <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                <Lock size={17} style={{ color: '#fe8704' }} />
                <input
                  type="password"
                  className="w-full border-none bg-transparent p-0 text-sm outline-none"
                  style={{ color: '#111827' }}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
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
              {loading ? t('auth.signingIn') : (<>{t('auth.signIn')} <LogIn size={16} /></>)}
            </button>

            <Link
              to="/forgot-password"
              className="text-center text-[13px] font-semibold"
              style={{ color: '#fe8704', marginTop: '-4px' }}
            >
              {t('auth.forgotPassword')}
            </Link>
          </form>

          <div className="mt-7 space-y-2.5 text-center text-[13px]" style={{ color: '#6b7280' }}>
            <p style={{ margin: 0 }}>
              {t('auth.areYouCustomer')}{' '}
              <Link to="/register" className="font-semibold" style={{ color: '#fe8704' }}>
                {t('auth.signUp')}
              </Link>
            </p>
            <p style={{ margin: 0 }}>
              {t('auth.wantToDrive')}{' '}
              <Link to="/apply" className="font-semibold" style={{ color: '#fe8704' }}>
                {t('auth.applyNow')}
              </Link>
            </p>
          </div>

          <Link
            to="/"
            className="mt-7 flex items-center justify-center gap-1.5 text-[13px] font-semibold"
            style={{ color: '#111827', marginTop: '28px' }}
          >
            <ArrowLeft size={14} /> {t('auth.backToHome')}
          </Link>
        </div>
      </Reveal>
    </div>
  );
}
