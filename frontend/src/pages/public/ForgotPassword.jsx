import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Mail, Lock, ShieldCheck, ArrowLeft, CheckCircle2 } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';
import PasswordStrength from '../../components/PasswordStrength.jsx';
import LanguageSwitcher from '../../components/LanguageSwitcher.jsx';
import { isPasswordStrong } from '../../utils/passwordRules.js';

import bannerHero from '../../assets/logzee/banner_slider_4.jpg';
import logoFooter from '../../assets/fleetra-mark.svg';

export default function ForgotPassword() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  // 'email' -> email daxil etmə addımı, 'reset' -> kod + yeni şifrə addımı.
  // Backend hansı halda olursa olsun eyni ümumi "kod göndərildi" mesajını
  // qaytarır (email mövcud olub-olmamasını sızdırmamaq üçün), ona görə burda
  // da uğur mesajı email-in real mövcudluğundan asılı olmayaraq eynidir.
  const [step, setStep] = useState('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  // Şifrə tələbləri qutusu yalnız sahə fokuslananda açılır (bax
  // PasswordStrength.jsx).
  const [passwordFocused, setPasswordFocused] = useState(false);

  const handleSendCode = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await axiosClient.post('/api/auth/forgot-password', { email });
      setStep('reset');
    } catch (err) {
      setError(err.response?.data?.message || t('auth.genericError'));
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await axiosClient.post('/api/auth/reset-password', { email, code, newPassword });
      setDone(true);
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(err.response?.data?.message || t('auth.genericError'));
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

          {step === 'email' && (
            <>
              <h2
                className="mt-7 text-center font-extrabold"
                style={{ color: '#111827', fontSize: '1.75rem', margin: '28px 0 0' }}
              >
                {t('auth.forgotTitle')}
              </h2>
              <p className="mt-2 text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>
                {t('auth.forgotSubtitle')}
              </p>

              {error && (
                <div
                  className="mt-5 rounded-lg px-4 py-3 text-sm"
                  style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}
                >
                  {error}
                </div>
              )}

              <form onSubmit={handleSendCode} className="mt-7 flex flex-col gap-4">
                <div>
                  <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                    {t('auth.emailLabel')}
                  </label>
                  <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                    <Mail size={17} style={{ color: '#fe8704' }} />
                    <input
                      type="email"
                      className="w-full border-none bg-transparent p-0 text-sm outline-none"
                      style={{ color: '#111827' }}
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
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
                  {loading ? t('auth.forgotSubmitting') : t('auth.forgotSubmit')}
                </button>
              </form>
            </>
          )}

          {step === 'reset' && (
            <>
              <h2
                className="mt-7 text-center font-extrabold"
                style={{ color: '#111827', fontSize: '1.75rem', margin: '28px 0 0' }}
              >
                {t('auth.resetTitle')}
              </h2>
              <p className="mt-2 text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>
                {t('auth.resetIntro')} — <strong>{email}</strong>
              </p>

              {error && (
                <div
                  className="mt-5 rounded-lg px-4 py-3 text-sm"
                  style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}
                >
                  {error}
                </div>
              )}
              {done && (
                <div
                  className="mt-5 flex items-center gap-2 rounded-lg px-4 py-3 text-sm"
                  style={{ background: '#f0fdf4', color: '#16a34a', border: '1px solid #bbf7d0' }}
                >
                  <CheckCircle2 size={16} /> {t('auth.resetSuccessMessage')}
                </div>
              )}

              <form onSubmit={handleReset} className="mt-7 flex flex-col gap-4">
                <div>
                  <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                    {t('auth.resetCodeLabel')}
                  </label>
                  <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                    <ShieldCheck size={17} style={{ color: '#fe8704' }} />
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={6}
                      className="w-full border-none bg-transparent p-0 text-center text-lg tracking-[0.5em] outline-none"
                      style={{ color: '#111827' }}
                      value={code}
                      onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                      required
                    />
                  </div>
                </div>

                <div style={{ position: 'relative' }}>
                  <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                    {t('auth.newPasswordLabel')}
                  </label>
                  <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                    <Lock size={17} style={{ color: '#fe8704' }} />
                    <input
                      type="password"
                      className="w-full border-none bg-transparent p-0 text-sm outline-none"
                      style={{ color: '#111827' }}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      onFocus={() => setPasswordFocused(true)}
                      onBlur={() => setPasswordFocused(false)}
                      required
                    />
                  </div>
                  <PasswordStrength password={newPassword} show={passwordFocused} />
                </div>

                <button
                  type="submit"
                  disabled={loading || code.length !== 6 || !isPasswordStrong(newPassword)}
                  className="mt-2 flex items-center justify-center gap-2 rounded-lg py-3.5 text-sm font-bold transition-transform hover:-translate-y-0.5 disabled:opacity-60"
                  style={{ background: '#fe8704', color: '#ffffff' }}
                >
                  {loading ? t('auth.resetSubmitting') : t('auth.resetSubmit')}
                </button>
              </form>
            </>
          )}

          <Link
            to="/login"
            className="mt-7 flex items-center justify-center gap-1.5 text-[13px] font-semibold"
            style={{ color: '#111827', marginTop: '28px' }}
          >
            <ArrowLeft size={14} /> {t('auth.backToLogin')}
          </Link>
        </div>
      </Reveal>
    </div>
  );
}
